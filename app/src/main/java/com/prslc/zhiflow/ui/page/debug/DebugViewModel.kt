package com.prslc.zhiflow.ui.page.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.core.network.HttpLogStore
import com.prslc.zhiflow.core.network.newDeviceId

class DebugViewModel(
    private val credentialStore: CredentialStore,
    private val httpLogStore: HttpLogStore,
) : ViewModel() {

    var xUdid by mutableStateOf(credentialStore.udid())
        private set

    var loggingEnabled by mutableStateOf(httpLogStore.isEnabled)
        private set

    fun toggleLogging() {
        val enabled = !loggingEnabled
        loggingEnabled = enabled
        httpLogStore.isEnabled = enabled
    }

    /** @param value Stored as-is, except that a blank one is refused by the store. */
    fun saveUdid(value: String) {
        credentialStore.replaceUdid(value)
        xUdid = credentialStore.udid()
    }

    /**
     * Replaces the device id with a fresh one, for when the stored one is in question.
     *
     * @return The new id, so the caller can show it without a second read.
     */
    fun regenerateUdid(): String {
        credentialStore.replaceUdid(newDeviceId())
        xUdid = credentialStore.udid()
        return xUdid
    }
}
