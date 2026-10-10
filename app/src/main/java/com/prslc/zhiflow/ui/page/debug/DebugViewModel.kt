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

    /**
     * Writes a session the server will refuse, so that the next request is rejected — and so is
     * the renewal that rejection triggers.
     *
     * Both tokens, not just the refresh one: while the access token is still good, a renewal is
     * answered with the session already held, which never reaches the fallback.
     *
     * @return False when there is no signed-in session.
     */
    fun breakSessionTokens(): Boolean {
        val session = credentialStore.read()
        if (session.isGuest || !session.hasToken) return false
        credentialStore.save(
            session.copy(accessToken = BROKEN_TOKEN, refreshToken = BROKEN_TOKEN)
        )
        return true
    }

    /**
     * Moves the session's next refresh into the past, so the next response renews it.
     *
     * The real schedule is weeks out, so this is the only way to reach that path without waiting
     * for it. Nothing is fetched here: the renewal is left to the code that would have run
     * anyway.
     *
     * @return False when there is no signed-in session, which is never refreshed.
     */
    fun makeRefreshDue(): Boolean {
        val session = credentialStore.read()
        if (session.isGuest || !session.hasToken) return false
        credentialStore.save(session.copy(nextRefreshAt = 0L))
        return true
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

    private companion object {
        const val BROKEN_TOKEN = "not-a-session-token"
    }
}
