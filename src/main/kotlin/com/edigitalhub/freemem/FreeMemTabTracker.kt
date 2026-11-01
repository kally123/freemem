package com.edigitalhub.freemem

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class FreeMemTabTracker : FileEditorManagerListener {

    private val accessTimes = ConcurrentHashMap<VirtualFile, Long>()

    companion object {
        fun getInstance(): FreeMemTabTracker = service<FreeMemTabTracker>()
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        touch(file)
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        accessTimes.remove(file)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        event.newFile?.let { touch(it) }
        event.oldFile?.let { touch(it) }
    }

    fun touch(file: VirtualFile) {
        accessTimes[file] = System.currentTimeMillis()
    }

    fun getIdleTabsToClose(project: Project, idleMinutes: Int): List<VirtualFile> {
        val fem = FileEditorManager.getInstance(project)
        val cutoff = System.currentTimeMillis() - (idleMinutes * 60_000L)
        val selectedFiles = fem.selectedFiles.toSet()
        val docManager = FileDocumentManager.getInstance()

        return fem.openFiles.filter { vFile ->
            // Do not close currently active/selected tabs
            if (vFile in selectedFiles) return@filter false

            // Do not close tabs with unsaved modifications
            if (docManager.isFileModified(vFile)) return@filter false

            // Check if idle based on tracked access time
            val lastAccess = accessTimes[vFile]
            if (lastAccess != null) {
                lastAccess < cutoff
            } else {
                // Untracked fallback: check file timestamp if not writable
                vFile.timeStamp < cutoff && !vFile.isWritable
            }
        }
    }
}
