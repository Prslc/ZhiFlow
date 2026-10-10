package com.prslc.zhiflow.data.session

import android.content.SharedPreferences
import androidx.core.content.edit
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.core.network.Credentials
import com.prslc.zhiflow.core.network.newDeviceId

/**
 * [CredentialStore] over [SharedPreferences] — the same file the debug screen's other settings
 * live in, which is why [clear] removes named keys rather than the file.
 */
class CredentialStoreImpl(private val sharedPreferences: SharedPreferences) : CredentialStore {

    /**
     * @return The stored device id, or a freshly generated one, stored before it is returned.
     *   Synchronized because generate-and-store is a read-modify-write: two threads racing on a
     *   fresh install would otherwise leave one of them holding an id that was never kept.
     */
    @Synchronized
    override fun udid(): String {
        sharedPreferences.getString(KEY_UDID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val generated = newDeviceId()
        sharedPreferences.edit { putString(KEY_UDID, generated) }
        return generated
    }

    /** @param value Ignored when blank, so a bad paste cannot blank the id for later requests. */
    override fun replaceUdid(value: String) {
        if (value.isBlank()) return
        sharedPreferences.edit { putString(KEY_UDID, value) }
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
