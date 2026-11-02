package com.edigitalhub.freemem

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class FreeMemAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        FreeMemService.getInstance().reclaimInBackground(e.project, "Manual reclaim completed")
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
