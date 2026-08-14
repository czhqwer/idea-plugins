package dev.czh.idea.ctool.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "DevDockSettings", storages = [Storage("devdock.xml")])
class DevDockSettings : SimplePersistentStateComponent<DevDockSettings.State>(State()) {
    class State : BaseState() {
        var lastToolId by string("json")

        var favoriteToolIds by string("hash,json,base64,time,regex,text")
    }

    var lastToolId: String
        get() = state.lastToolId ?: "json"
        set(value) {
            state.lastToolId = value
        }

    var favoriteToolIds: List<String>
        get() = (state.favoriteToolIds ?: "").split(',').map(String::trim).filter(String::isNotEmpty)
        set(value) {
            state.favoriteToolIds = value.joinToString(",")
        }
}

val devDockSettings: DevDockSettings
    get() = service()
