package com.prslc.zhiflow.core.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

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

    private val _readingTextScale = MutableStateFlow(readReadingTextScale())

    private val _readingLetterSpacing = MutableStateFlow(readReadingLetterSpacing())

    private val _readingLineHeight = MutableStateFlow(readReadingLineHeight())

    private val _readingMargin = MutableStateFlow(readReadingMargin())

    private val _segmentComments = MutableStateFlow(readSegmentComments())

    private val _developerUnlocked = MutableStateFlow(readDeveloperUnlocked())

    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override val dataSaver: StateFlow<Boolean> = _dataSaver.asStateFlow()

    override val searchHistory: StateFlow<Boolean> = _searchHistory.asStateFlow()

    override val readingProgress: StateFlow<Boolean> = _readingProgress.asStateFlow()

    override val barsFollowScroll: StateFlow<Boolean> = _barsFollowScroll.asStateFlow()

    override val readingTextScale: StateFlow<Float> = _readingTextScale.asStateFlow()

    override val readingLetterSpacing: StateFlow<Float> = _readingLetterSpacing.asStateFlow()

    override val readingLineHeight: StateFlow<Float> = _readingLineHeight.asStateFlow()

    override val readingMargin: StateFlow<Float> = _readingMargin.asStateFlow()

    override val segmentComments: StateFlow<Boolean> = _segmentComments.asStateFlow()

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

    override fun setReadingTextScale(scale: Float) {
        preferences.edit { putFloat(KEY_READING_TEXT_SCALE, scale) }
        _readingTextScale.value = scale
    }

    override fun setReadingLetterSpacing(spacing: Float) {
        preferences.edit { putFloat(KEY_READING_LETTER_SPACING, spacing) }
        _readingLetterSpacing.value = spacing
    }

    override fun setReadingLineHeight(scale: Float) {
        preferences.edit { putFloat(KEY_READING_LINE_HEIGHT, scale) }
        _readingLineHeight.value = scale
    }

    override fun setReadingMargin(margin: Float) {
        preferences.edit { putFloat(KEY_READING_MARGIN_DP, margin) }
        _readingMargin.value = margin
    }

    override fun setSegmentComments(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_SEGMENT_COMMENTS, enabled) }
        _segmentComments.value = enabled
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

    /**
     * @return The stored multiple, or the system's own when nothing readable is stored.
     *
     * The size was once one of four named steps; an install that still has one of those stored has
     * never written the multiple, so its name is read for the value it stood for.
     */
    private fun readReadingTextScale(): Float {
        val stored = preferences.getFloat(KEY_READING_TEXT_SCALE, Float.NaN)
        val scale = if (stored.isNaN()) legacyTextScale() else stored
        return scale.nearestTo(ReadingChoices.textScales)
    }

    /** @return The multiple the named text size stood for, or the system's own. */
    private fun legacyTextScale(): Float = when (storedName(KEY_LEGACY_READING_TEXT_SIZE)) {
        "Small" -> 0.85f
        "Large" -> 1.15f
        "ExtraLarge" -> 1.3f
        else -> 1f
    }

    /** @return The stored spacing, or none added when nothing readable is stored. */
    private fun readReadingLetterSpacing(): Float =
        preferences.getFloat(KEY_READING_LETTER_SPACING, 0f)
            .nearestTo(ReadingChoices.letterSpacings)

    /** @return The stored multiple of a style's line height, or its own height when nothing is. */
    private fun readReadingLineHeight(): Float =
        preferences.getFloat(KEY_READING_LINE_HEIGHT, 1f)
            .nearestTo(ReadingChoices.lineHeights)

    /**
     * @return The choice nearest this value, so that what is in force is always one the page
     *   offers: a value stored by an older build, or by one that offered a finer range, still
     *   lands on a choice rather than between two of them.
     */
    private fun Float.nearestTo(choices: List<Float>): Float = choices.minBy { abs(it - this) }

    /**
     * @return The stored margin, or the one the page has always drawn at.
     *
     * The margin was once one of three named rooms; an install that still has one of those stored
     * has never written the room in dp, so its name is read for the value it stood for.
     */
    private fun readReadingMargin(): Float {
        val stored = preferences.getFloat(KEY_READING_MARGIN_DP, Float.NaN)
        val margin = if (stored.isNaN()) legacyMargin() else stored
        return margin.nearestTo(ReadingChoices.margins)
    }

    /** @return The room the named margin stood for, or the one the page has always drawn at. */
    private fun legacyMargin(): Float = when (storedName(KEY_LEGACY_READING_MARGIN)) {
        "Narrow" -> 12f
        "Wide" -> 32f
        else -> 20f
    }

    /** @return The stored choice, or true when nothing is stored — the marks have always been drawn. */
    private fun readSegmentComments(): Boolean = preferences.getBoolean(KEY_SEGMENT_COMMENTS, true)

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
        const val KEY_READING_TEXT_SCALE = "reading_text_scale"
        const val KEY_READING_LETTER_SPACING = "reading_letter_spacing"
        const val KEY_READING_LINE_HEIGHT = "reading_line_height"
        const val KEY_READING_MARGIN_DP = "reading_margin_dp"
        const val KEY_SEGMENT_COMMENTS = "segment_comments_enabled"

        /** The key the text size was stored under while it was one of four named steps. */
        const val KEY_LEGACY_READING_TEXT_SIZE = "reading_text_size"

        /** The key the margin was stored under while it was one of three named rooms. */
        const val KEY_LEGACY_READING_MARGIN = "reading_margin"
        const val KEY_DEVELOPER_UNLOCKED = "developer_unlocked"
    }
}
