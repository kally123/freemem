package com.edigitalhub.freemem

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class FreeMemConfigurable : BoundConfigurable("freemem") {

    private val settings = FreeMemSettingsState.getInstance()

    override fun createPanel(): DialogPanel = panel {
        val state = settings.state

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
    }
}
