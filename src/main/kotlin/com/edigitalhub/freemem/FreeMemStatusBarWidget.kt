package com.edigitalhub.freemem

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class FreeMemStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {

    companion object {
        const val ID = "freemem.StatusBarWidget"
    }

    private var myStatusBar: StatusBar? = null
    private var timerFuture: ScheduledFuture<*>? = null

    init {
        timerFuture = AppExecutorUtil.getAppScheduledExecutorService()
            .scheduleWithFixedDelay({ update() }, 5, 10, TimeUnit.SECONDS)
    }

    override fun ID(): String = ID

    override fun install(statusBar: StatusBar) {
        myStatusBar = statusBar
        update()
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String {
        val used = FreeMemService.usedMB()
        val max = FreeMemService.maxMB()
        val pct = FreeMemService.usedPercent()
        return "freemem: $pct% ($used/${max}MB)"
    }

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getTooltipText(): String {
        val service = FreeMemService.getInstance()
        return "freemem: Click to reclaim memory or open settings\n" +
                "Heap usage: ${FreeMemService.usedPercent()}%\n" +
                "Total freed this session: ${service.totalFreedMB.get()} MB (${service.reclaimCount.get()} reclaims)"
    }

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer { e ->
        val component = e.component
        val actionManager = ActionManager.getInstance()
        val group = DefaultActionGroup(
            actionManager.getAction("freemem.FreeMemAction"),
            actionManager.getAction("freemem.FreeMemSettingsAction")
        )
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "freemem",
            group,
            DataManager.getInstance().getDataContext(component),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
        popup.show(RelativePoint(component, Point(0, -popup.content.preferredSize.height)))
    }

    fun update() {
        myStatusBar?.updateWidget(ID())
    }

    override fun dispose() {
        timerFuture?.cancel(false)
        myStatusBar = null
    }
}
