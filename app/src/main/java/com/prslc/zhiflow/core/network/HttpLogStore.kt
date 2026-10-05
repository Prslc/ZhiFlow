package com.prslc.zhiflow.core.network

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory, bounded record of API traffic for the in-app log screen.
 *
 * Entries never touch disk and never reach logcat: response bodies can carry
 * personal data, and logcat's noise is the problem this exists to solve.
 */
class HttpLogStore(private val sharedPreferences: SharedPreferences) {

    private val _entries = MutableStateFlow<List<HttpLogEntry>>(emptyList())
    val entries: StateFlow<List<HttpLogEntry>> = _entries.asStateFlow()

    @Volatile
    var isEnabled: Boolean = sharedPreferences.getBoolean(KEY_ENABLED, false)
        set(value) {
            field = value
            sharedPreferences.edit { putBoolean(KEY_ENABLED, value) }
        }

    /** Prepends [entry], newest first. Safe to call from any thread. */
    fun add(entry: HttpLogEntry) {
        _entries.update { current -> (listOf(entry) + current).take(MAX_ENTRIES) }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    private companion object {
        const val KEY_ENABLED = "http_log_enabled"
        const val MAX_ENTRIES = 100
    }
}
