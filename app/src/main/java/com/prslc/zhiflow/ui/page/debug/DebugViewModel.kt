package com.prslc.zhiflow.ui.page.debug

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.core.content.edit
import com.prslc.zhiflow.data.session.UserSession

class DebugViewModel(
    private val sharedPreferences: SharedPreferences,
    private val userSession: UserSession,
) : ViewModel() {
    var authorization by mutableStateOf(sharedPreferences.getString("auth", "") ?: "")
    var cookie by mutableStateOf(sharedPreferences.getString("cookie", "") ?: "")
    var xUdid by mutableStateOf(sharedPreferences.getString("x_udid", "") ?: "")

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
        sharedPreferences.edit { clear() }
        userSession.invalidate()
    }
}
