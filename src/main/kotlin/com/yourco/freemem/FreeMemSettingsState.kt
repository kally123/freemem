package com.yourco.freemem

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "FreeMemSettings", storages = [Storage("freemem.xml")])
class FreeMemSettingsState : PersistentStateComponent<FreeMemSettingsState.State> {

    data class State(
        var thresholdPercent: Int = 85,
        var checkIntervalMinutes: Int = 2,
        var dropPsiCaches: Boolean = true,
        var closeIdleTabs: Boolean = false,
        var idleTabMinutes: Int = 30
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    companion object {
        fun getInstance(): FreeMemSettingsState =
            ApplicationManager.getApplication()
                .getService(FreeMemSettingsState::class.java)
    }
}
