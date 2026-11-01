package com.edigitalhub.freemem

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic

@State(name = "FreeMemSettings", storages = [Storage("freemem.xml")])
class FreeMemSettingsState : PersistentStateComponent<FreeMemSettingsState.State> {

    interface Listener {
        fun settingsChanged(state: State)
    }

    companion object {
        val TOPIC = Topic.create("FreeMemSettingsChanged", Listener::class.java)

        fun getInstance(): FreeMemSettingsState =
            ApplicationManager.getApplication().getService(FreeMemSettingsState::class.java)
    }

    data class State(
        var enableAutoReclaim: Boolean = true,
        var thresholdPercent: Int = 85,
        var checkIntervalMinutes: Int = 2,
        var dropPsiCaches: Boolean = true,
        var closeIdleTabs: Boolean = false,
        var idleTabMinutes: Int = 30,
        var notifyOnAutoReclaim: Boolean = false
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun fireSettingsChanged() {
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).settingsChanged(myState)
    }
}
