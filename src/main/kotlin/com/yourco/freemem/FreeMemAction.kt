package com.yourco.freemem

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiManager

class FreeMemAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val beforeMB = usedMB()
        System.gc()
        project?.let { PsiManager.getInstance(it).dropPsiCaches() }
        val afterMB  = usedMB()
        val freedMB  = beforeMB - afterMB

        Notifications.Bus.notify(
            Notification(
                "freemem",
                "freemem: memory freed",
                "Freed ≈${freedMB} MB · Heap now ${afterMB} MB / ${maxMB()} MB",
                NotificationType.INFORMATION
            ),
            project
        )
    }

    private fun usedMB() = Runtime.getRuntime().let {
        (it.totalMemory() - it.freeMemory()) / 1_048_576
    }
    private fun maxMB() = Runtime.getRuntime().maxMemory() / 1_048_576
}
