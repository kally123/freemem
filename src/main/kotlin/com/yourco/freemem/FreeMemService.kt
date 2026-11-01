package com.yourco.freemem

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class FreeMemService : com.intellij.openapi.Disposable {

    private val log = thisLogger()
    private var future: ScheduledFuture<*>? = null

    init { reschedule() }

    fun reschedule() {
        future?.cancel(false)
        val interval = FreeMemSettingsState.getInstance().state.checkIntervalMinutes.toLong()
        future = AppExecutorUtil.getAppScheduledExecutorService()
            .scheduleWithFixedDelay(::checkAndFree, interval, interval, TimeUnit.MINUTES)
        log.info("freemem: scheduled every $interval min")
    }

    private fun checkAndFree() {
        val settings = FreeMemSettingsState.getInstance().state
        val rt = Runtime.getRuntime()
        val usedPct = ((rt.totalMemory() - rt.freeMemory()).toDouble() / rt.maxMemory() * 100).toInt()

        if (usedPct < settings.thresholdPercent) return
        log.info("freemem: heap at $usedPct% — freeing")
        System.gc()

        if (settings.dropPsiCaches) {
            ApplicationManager.getApplication().runReadAction {
                ProjectManager.getInstance().openProjects.forEach { project ->
                    if (!project.isDisposed) PsiManager.getInstance(project).dropPsiCaches()
                }
            }
        }

        if (settings.closeIdleTabs) {
            ApplicationManager.getApplication().invokeLater {
                closeIdleEditorTabs(settings.idleTabMinutes)
            }
        }
    }

    private fun closeIdleEditorTabs(idleMinutes: Int) {
        val cutoff = System.currentTimeMillis() - idleMinutes * 60_000L
        ProjectManager.getInstance().openProjects.forEach { project ->
            if (project.isDisposed) return@forEach
            val fem = FileEditorManager.getInstance(project)
            fem.openFiles.forEach { vFile ->
                if (vFile.timeStamp < cutoff && !vFile.isWritable) {
                    ApplicationManager.getApplication().invokeLater { fem.closeFile(vFile) }
                }
            }
        }
    }

    override fun dispose() { future?.cancel(false) }
}
