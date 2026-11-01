package com.edigitalhub.freemem

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class FreeMemAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val service = FreeMemService.getInstance()
        val stats = service.performReclaim(isManual = true, targetProject = project)
        service.notifyStats(stats, project, "Manual reclaim completed")
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
