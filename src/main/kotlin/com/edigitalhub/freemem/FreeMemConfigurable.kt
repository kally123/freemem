package com.edigitalhub.freemem

import com.intellij.diagnostic.VMOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import java.io.IOException

class FreeMemConfigurable : BoundConfigurable("freemem") {

    private val settings = FreeMemSettingsState.getInstance()

    // -Xmx as written in the IDE's .vmoptions file; falls back to the running JVM's max heap
    private var savedMaxHeapMB = configuredMaxHeapMB()
    private var maxHeapMB = savedMaxHeapMB

    private fun configuredMaxHeapMB(): Int =
        VMOptions.readOption(VMOptions.MemoryKind.HEAP, true).takeIf { it > 0 }
            ?: FreeMemService.maxMB().toInt()

    override fun createPanel(): DialogPanel = panel {
        val state = settings.state

        group("IDE Memory Limit") {
            row("Maximum heap size (-Xmx, MB):") {
                intTextField(256..65536, keyboardStep = 256)
                    .bindIntText(::maxHeapMB)
                    .enabled(VMOptions.canWriteOptions())
                    .comment("Written to the IDE's .vmoptions file. Takes effect after an IDE restart.")
            }
            row("Running JVM max heap:") {
                label("${FreeMemService.maxMB()} MB")
            }
        }

        group("Automatic Memory Reclaim") {
            row {
                checkBox("Enable background automatic reclaim")
                    .bindSelected(state::enableAutoReclaim)
                    .comment("Monitors IDE heap in the background and frees memory when threshold is reached")
            }
            row("Heap usage threshold (% of max):") {
                spinner(50..98, step = 5)
                    .bindIntValue(state::thresholdPercent)
                    .comment("Trigger reclaim when heap reaches this percentage")
            }
            row("Check interval (minutes):") {
                spinner(1..60, step = 1)
                    .bindIntValue(state::checkIntervalMinutes)
            }
            row {
                checkBox("Drop PSI caches during automatic reclaim")
                    .bindSelected(state::dropPsiCaches)
                    .comment("Releases syntax tree and indexing caches across all open projects")
            }
            row {
                checkBox("Show notification on automatic reclaim")
                    .bindSelected(state::notifyOnAutoReclaim)
            }
        }

        group("Editor Tab Hygiene") {
            row {
                checkBox("Close stale editor tabs when freeing memory")
                    .bindSelected(state::closeIdleTabs)
                    .comment("Closes inactive, unmodified tabs to reduce editor memory footprint")
            }
            row("Idle time cutoff (minutes):") {
                spinner(5..180, step = 5)
                    .bindIntValue(state::idleTabMinutes)
            }
        }

        group("Session Statistics") {
            val service = FreeMemService.getInstance()
            row("Total memory freed this session:") {
                label("${service.totalFreedMB.get()} MB across ${service.reclaimCount.get()} reclaims")
            }
            row("Current heap:") {
                label("${FreeMemService.usedMB()} MB / ${FreeMemService.maxMB()} MB (${FreeMemService.usedPercent()}%)")
            }
        }
    }

    override fun apply() {
        super.apply()
        settings.fireSettingsChanged()

        if (maxHeapMB != savedMaxHeapMB) {
            try {
                VMOptions.setOption(VMOptions.MemoryKind.HEAP, maxHeapMB)
            } catch (e: IOException) {
                throw ConfigurationException("Could not write -Xmx to the .vmoptions file: ${e.message}")
            }
            savedMaxHeapMB = maxHeapMB
            ApplicationManager.getApplication().invokeLater {
                val restart = Messages.showYesNoDialog(
                    "Maximum heap set to $maxHeapMB MB. Restart the IDE now to apply it?",
                    "freemem",
                    "Restart Now",
                    "Later",
                    Messages.getQuestionIcon()
                )
                if (restart == Messages.YES) ApplicationManager.getApplication().restart()
            }
        }
    }
}
