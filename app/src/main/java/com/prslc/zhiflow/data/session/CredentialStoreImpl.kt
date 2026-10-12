package com.prslc.zhiflow.data.session

import android.content.SharedPreferences
import androidx.core.content.edit
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.core.network.Credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [CredentialStore] over [SharedPreferences] — the same file the debug screen's other settings
 * live in, which is why [clear] removes named keys rather than the file.
 */
class CredentialStoreImpl(private val sharedPreferences: SharedPreferences) : CredentialStore {

    // Seeded from what is stored rather than from an empty session: a collector that arrives
    // after the bootstrap has already landed would otherwise wait for a change that never comes.
    private val _session = MutableStateFlow(read())

    override val session: StateFlow<Credentials> = _session.asStateFlow()

    /** @return The stored device id, or null while the bootstrap has not run. */
    override fun udid(): String? =
        sharedPreferences.getString(KEY_UDID, null)?.takeIf { it.isNotBlank() }

    /** @param value Ignored when blank, so a bad paste cannot blank the id for later requests. */
    override fun replaceUdid(value: String) {
        if (value.isBlank()) return
        sharedPreferences.edit { putString(KEY_UDID, value) }
    }

    override fun clearUdid() {
        sharedPreferences.edit { remove(KEY_UDID) }
    }

    /** @return The stored session; all-empty on a fresh install. */
    override fun read(): Credentials = Credentials(
        accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, "").orEmpty(),
        refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, "").orEmpty(),
        userId = sharedPreferences.getString(KEY_USER_ID, "").orEmpty(),
        userType = sharedPreferences.getString(KEY_USER_TYPE, "").orEmpty(),
        nextRefreshAt = sharedPreferences.getLong(KEY_NEXT_REFRESH_AT, Credentials.NEVER),
    )

    /** @param credentials Written as one batch, so a concurrent read sees all of them or none. */
    override fun save(credentials: Credentials) {
        sharedPreferences.edit {
            putString(KEY_ACCESS_TOKEN, credentials.accessToken)
            putString(KEY_REFRESH_TOKEN, credentials.refreshToken)
            putString(KEY_USER_ID, credentials.userId)
            putString(KEY_USER_TYPE, credentials.userType)
            putLong(KEY_NEXT_REFRESH_AT, credentials.nextRefreshAt)
        }
        _session.value = credentials
    }

    /** Leaves `http_log_enabled` and the device id alone: both outlive the session. */
    override fun clear() {
        sharedPreferences.edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_TYPE)
            remove(KEY_NEXT_REFRESH_AT)
        }
        _session.value = read()
    }

    private companion object {
        // The device id keeps the key the debug screen has always written, so an id pasted
        // before this existed survives. The old `auth` and `cookie` keys are not read again:
        // the login screen replaces the first, and the cookie jar the second.
        const val KEY_UDID = "x_udid"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_TYPE = "user_type"
        const val KEY_NEXT_REFRESH_AT = "next_refresh_at"
    }
}
