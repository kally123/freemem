package com.edigitalhub.freemem

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.LowMemoryWatcher
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Service(Service.Level.APP)
class FreeMemService : Disposable {

    private val log = thisLogger()
    private var future: ScheduledFuture<*>? = null

    val totalFreedMB = AtomicLong(0)
    val reclaimCount = AtomicLong(0)

    data class ReclaimStats(
        val beforeMB: Long,
        val afterMB: Long,
        val freedMB: Long,
        val maxMB: Long,
        val closedTabsCount: Int
    )

    init {
        // Listen for settings modifications
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(FreeMemSettingsState.TOPIC, object : FreeMemSettingsState.Listener {
                override fun settingsChanged(state: FreeMemSettingsState.State) {
                    reschedule()
                }
            })

        // Emergency reclaim when JVM encounters native low memory conditions
        LowMemoryWatcher.register({
            log.warn("freemem: Low memory event detected by IDE runtime! Running emergency cleanup.")
            // Reclaim on a pooled thread so the platform's other low-memory listeners aren't held up
            AppExecutorUtil.getAppExecutorService().execute {
                performReclaim(isManual = false, targetProject = null)
            }
        }, this)

        reschedule()
    }

    companion object {
        fun getInstance(): FreeMemService = service<FreeMemService>()

        fun usedMB(): Long = Runtime.getRuntime().let {
            (it.totalMemory() - it.freeMemory()) / 1_048_576
        }

        fun maxMB(): Long = Runtime.getRuntime().maxMemory() / 1_048_576

        fun usedPercent(): Int {
            val rt = Runtime.getRuntime()
            return ((rt.totalMemory() - rt.freeMemory()).toDouble() / rt.maxMemory() * 100).toInt()
        }
    }

    fun reschedule() {
        future?.cancel(false)
        val settings = FreeMemSettingsState.getInstance().state

        if (!settings.enableAutoReclaim) {
            log.info("freemem: automatic reclaim is disabled")
            return
        }

        val interval = maxOf(1, settings.checkIntervalMinutes).toLong()
        future = AppExecutorUtil.getAppScheduledExecutorService()
            .scheduleWithFixedDelay(::checkAndFree, interval, interval, TimeUnit.MINUTES)
        log.info("freemem: scheduled check every $interval min (threshold: ${settings.thresholdPercent}%)")
    }

    private fun checkAndFree() {
        val settings = FreeMemSettingsState.getInstance().state
        if (!settings.enableAutoReclaim) return

        val usedPct = usedPercent()
        if (usedPct < settings.thresholdPercent) return

        log.info("freemem: heap at $usedPct% (exceeds threshold ${settings.thresholdPercent}%) — initiating reclaim")
        // An exception escaping here would cancel the fixed-delay schedule and stop all future checks
        val stats = try {
            performReclaim(isManual = false, targetProject = null)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            log.warn("freemem: automatic reclaim failed", e)
            return
        }

        if (settings.notifyOnAutoReclaim) {
            notifyStats(stats, null, "Auto-reclaim triggered ($usedPct% heap)")
        }
    }

    /** Runs a manual reclaim as a background task so the GC never blocks the EDT. */
    fun reclaimInBackground(project: Project?, title: String) {
        object : Task.Backgroundable(project, "Freeing IDE memory", false) {
            private lateinit var stats: ReclaimStats

            override fun run(indicator: ProgressIndicator) {
                stats = performReclaim(isManual = true, targetProject = project)
            }

            override fun onSuccess() {
                notifyStats(stats, project, title)
                project?.let { WindowManager.getInstance().getStatusBar(it)?.updateWidget(FreeMemStatusBarWidget.ID) }
            }
        }.queue()
    }

    /** Hops to the EDT only for document, PSI, and editor work; call it off the EDT. */
    fun performReclaim(isManual: Boolean, targetProject: Project?): ReclaimStats {
        val settings = FreeMemSettingsState.getInstance().state
        val beforeMB = usedMB()

        ApplicationManager.getApplication().invokeAndWait {
            // 1. Flush documents to disk to ensure PSI caches can be safely invalidated
            FileDocumentManager.getInstance().saveAllDocuments()

            // 2. Drop PSI caches across projects (dropPsiCaches is EDT-only)
            if (settings.dropPsiCaches || isManual) {
                val projects = if (targetProject != null && !isManual) {
                    listOf(targetProject)
                } else {
                    ProjectManager.getInstance().openProjects.toList()
                }
                for (p in projects) {
                    if (!p.isDisposed) {
                        PsiManager.getInstance(p).dropPsiCaches()
                    }
                }
            }
        }

        // 3. Close idle editor tabs if configured
        var closedTabs = 0
        if (settings.closeIdleTabs) {
            closedTabs = closeIdleTabsAcrossProjects(settings.idleTabMinutes)
        }

        // 4. Trigger garbage collection and finalization
        System.gc()
        System.runFinalization()
        System.gc()

        val afterMB = usedMB()
        val freedMB = maxOf(0L, beforeMB - afterMB)

        totalFreedMB.addAndGet(freedMB)
        reclaimCount.incrementAndGet()

        log.info("freemem: reclaimed ${freedMB}MB (before: ${beforeMB}MB, after: ${afterMB}MB, closed tabs: $closedTabs)")
        return ReclaimStats(beforeMB, afterMB, freedMB, maxMB(), closedTabs)
    }

    private fun closeIdleTabsAcrossProjects(idleMinutes: Int): Int {
        var closedCount = 0
        val tracker = FreeMemTabTracker.getInstance()

        ApplicationManager.getApplication().invokeAndWait {
            for (project in ProjectManager.getInstance().openProjects) {
                if (project.isDisposed) continue
                val idleFiles = tracker.getIdleTabsToClose(project, idleMinutes)
                val fem = FileEditorManager.getInstance(project)
                for (file in idleFiles) {
                    fem.closeFile(file)
                    closedCount++
                }
            }
        }
        return closedCount
    }

    fun notifyStats(stats: ReclaimStats, project: Project?, title: String = "freemem: memory freed") {
        val tabDetail = if (stats.closedTabsCount > 0) " · Closed ${stats.closedTabsCount} idle tab(s)" else ""
        val content = "Freed ≈${stats.freedMB} MB · Heap now ${stats.afterMB} MB / ${stats.maxMB} MB (${usedPercent()}%)$tabDetail"

        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("freemem")
                .createNotification(title, content, NotificationType.INFORMATION)
                .notify(project)
        } catch (e: Exception) {
            log.warn("freemem: failed to show notification: ${e.message}")
        }
    }

    override fun dispose() {
        future?.cancel(false)
    }
}
