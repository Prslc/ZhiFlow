package com.prslc.zhiflow.ui.page.debug

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.core.content.edit
import com.prslc.zhiflow.core.network.HttpLogStore
import com.prslc.zhiflow.data.session.UserSession

class DebugViewModel(
    private val sharedPreferences: SharedPreferences,
    private val userSession: UserSession,
    private val httpLogStore: HttpLogStore,
) : ViewModel() {
    var authorization by mutableStateOf(sharedPreferences.getString("auth", "") ?: "")
    var cookie by mutableStateOf(sharedPreferences.getString("cookie", "") ?: "")
    var xUdid by mutableStateOf(sharedPreferences.getString("x_udid", "") ?: "")

    var loggingEnabled by mutableStateOf(httpLogStore.isEnabled)
        private set

    fun toggleLogging() {
        val enabled = !loggingEnabled
        loggingEnabled = enabled
        httpLogStore.isEnabled = enabled
    }

    fun save() {
        sharedPreferences.edit {
            putString("auth", authorization)
            putString("cookie", cookie)
            putString("x_udid", xUdid)
        }
        userSession.invalidate()
    }

    fun clear() {
        authorization = ""
        cookie = ""
        xUdid = ""
        // Remove only the credential keys, so unrelated preferences survive.
        sharedPreferences.edit {
            remove("auth")
            remove("cookie")
            remove("x_udid")
        }
        userSession.invalidate()
    }
}
