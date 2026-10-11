package com.prslc.zhiflow.core.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [SettingsStore] over [SharedPreferences].
 *
 * The state flow is seeded from what is stored rather than from a default, so a collector that
 * arrives after the page has already been used still reads the setting that is in force.
 */
class SettingsStoreImpl(private val preferences: SharedPreferences) : SettingsStore {

    private val _themeMode = MutableStateFlow(readThemeMode())

    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        preferences.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    /** @return The stored theme, or [ThemeMode.System] when nothing readable is stored. */
    private fun readThemeMode(): ThemeMode = preferences.getString(KEY_THEME_MODE, null)
        ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
        ?: ThemeMode.System

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
