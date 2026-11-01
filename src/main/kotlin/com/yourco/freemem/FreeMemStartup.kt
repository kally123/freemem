package com.yourco.freemem

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** Instantiates the app-level service so its scheduler starts once a project opens. */
class FreeMemStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        service<FreeMemService>()
    }
}
