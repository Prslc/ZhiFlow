package com.prslc.zhiflow.ui.page.debug

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.core.content.edit

class DebugViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {
    var authorization by mutableStateOf(sharedPreferences.getString("auth", "") ?: "")
    var cookie by mutableStateOf(sharedPreferences.getString("cookie", "") ?: "")
    var xUdid by mutableStateOf(sharedPreferences.getString("x_udid", "") ?: "")

    fun save() {
        sharedPreferences.edit {
            putString("auth", authorization)
            putString("cookie", cookie)
            putString("x_udid", xUdid)
        }
    }

    fun clear() {
        authorization = ""
        cookie = ""
        xUdid = ""
        sharedPreferences.edit { clear() }
    }
}
