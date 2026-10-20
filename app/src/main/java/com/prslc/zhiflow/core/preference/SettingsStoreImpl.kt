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

    private val _dataSaver = MutableStateFlow(readDataSaver())

    private val _searchHistory = MutableStateFlow(readSearchHistory())

    private val _readingProgress = MutableStateFlow(readReadingProgress())

    private val _barsFollowScroll = MutableStateFlow(readBarsFollowScroll())

    private val _readingTextSize = MutableStateFlow(readReadingTextSize())

    private val _readingMargin = MutableStateFlow(readReadingMargin())

    private val _developerUnlocked = MutableStateFlow(readDeveloperUnlocked())

    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override val dataSaver: StateFlow<Boolean> = _dataSaver.asStateFlow()

    override val searchHistory: StateFlow<Boolean> = _searchHistory.asStateFlow()

    override val readingProgress: StateFlow<Boolean> = _readingProgress.asStateFlow()

    override val barsFollowScroll: StateFlow<Boolean> = _barsFollowScroll.asStateFlow()

    override val readingTextSize: StateFlow<ReadingTextSize> = _readingTextSize.asStateFlow()

    override val readingMargin: StateFlow<ReadingMargin> = _readingMargin.asStateFlow()

    override val developerUnlocked: StateFlow<Boolean> = _developerUnlocked.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        preferences.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    override fun setDataSaver(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_DATA_SAVER, enabled) }
        _dataSaver.value = enabled
    }

    override fun setSearchHistory(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_SEARCH_HISTORY, enabled) }
        _searchHistory.value = enabled
    }

    override fun setReadingProgress(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_READING_PROGRESS, enabled) }
        _readingProgress.value = enabled
    }

    override fun setBarsFollowScroll(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_BARS_FOLLOW_SCROLL, enabled) }
        _barsFollowScroll.value = enabled
    }

    override fun setReadingTextSize(size: ReadingTextSize) {
        preferences.edit { putString(KEY_READING_TEXT_SIZE, size.name) }
        _readingTextSize.value = size
    }

    override fun setReadingMargin(margin: ReadingMargin) {
        preferences.edit { putString(KEY_READING_MARGIN, margin.name) }
        _readingMargin.value = margin
    }

    override fun setDeveloperUnlocked(unlocked: Boolean) {
        preferences.edit { putBoolean(KEY_DEVELOPER_UNLOCKED, unlocked) }
        _developerUnlocked.value = unlocked
    }

    /** @return The stored theme, or [ThemeMode.System] when nothing readable is stored. */
    private fun readThemeMode(): ThemeMode = storedName(KEY_THEME_MODE)
        ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
        ?: ThemeMode.System

    /** @return The stored choice, or false when nothing is stored — a fresh install loads images. */
    private fun readDataSaver(): Boolean = preferences.getBoolean(KEY_DATA_SAVER, false)

    /** @return The stored choice, or true when nothing is stored — a fresh install offers it. */
    private fun readSearchHistory(): Boolean = preferences.getBoolean(KEY_SEARCH_HISTORY, true)

    /** @return The stored choice, or true when nothing is stored — the page has always drawn it. */
    private fun readReadingProgress(): Boolean =
        preferences.getBoolean(KEY_READING_PROGRESS, true)

    /** @return The stored choice, or true when nothing is stored — the bars have always followed. */
    private fun readBarsFollowScroll(): Boolean =
        preferences.getBoolean(KEY_BARS_FOLLOW_SCROLL, true)

    /** @return The stored size, or the system's own when nothing readable is stored. */
    private fun readReadingTextSize(): ReadingTextSize =
        storedName(KEY_READING_TEXT_SIZE)
            ?.let { stored -> ReadingTextSize.entries.firstOrNull { it.name == stored } }
            ?: ReadingTextSize.Standard

    /** @return The stored margin, or the one the page has always drawn at. */
    private fun readReadingMargin(): ReadingMargin =
        storedName(KEY_READING_MARGIN)
            ?.let { stored -> ReadingMargin.entries.firstOrNull { it.name == stored } }
            ?: ReadingMargin.Standard

    /** @return What is stored under [key], or null when nothing is. */
    private fun storedName(key: String): String? = preferences.getString(key, null)

    /** @return Whether the tools were unlocked, which stays true once it is. */
    private fun readDeveloperUnlocked(): Boolean =
        preferences.getBoolean(KEY_DEVELOPER_UNLOCKED, false)

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DATA_SAVER = "data_saver"
        const val KEY_SEARCH_HISTORY = "search_history_enabled"
        const val KEY_READING_PROGRESS = "reading_progress_enabled"
        const val KEY_BARS_FOLLOW_SCROLL = "bars_follow_scroll"
        const val KEY_READING_TEXT_SIZE = "reading_text_size"
        const val KEY_READING_MARGIN = "reading_margin"
        const val KEY_DEVELOPER_UNLOCKED = "developer_unlocked"
    }
}
