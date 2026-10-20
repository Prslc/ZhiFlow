package com.prslc.zhiflow.ui.page.profile

import androidx.lifecycle.ViewModel
import com.prslc.zhiflow.core.preference.ReadingMargin
import com.prslc.zhiflow.core.preference.SettingsStore
import kotlinx.coroutines.flow.StateFlow

/**
 * The reading settings page: what a content page draws, and whether its bars step aside.
 *
 * Both choices are published by the store, which is what lets a change here reach a page already
 * open behind this one.
 *
 * @param settingsStore Where the choices are kept.
 */
class ReadingSettingsViewModel(private val settingsStore: SettingsStore) : ViewModel() {

    /** Whether a content page draws its reading progress bar. */
    val readingProgress: StateFlow<Boolean> = settingsStore.readingProgress

    /** Whether a content page's bars leave with the scroll. */
    val barsFollowScroll: StateFlow<Boolean> = settingsStore.barsFollowScroll

    /** How large a content page's text draws, as a multiple of the system's own. */
    val readingTextScale: StateFlow<Float> = settingsStore.readingTextScale

    /** How far apart a content page sets its characters, in sp on top of what prose already uses. */
    val readingLetterSpacing: StateFlow<Float> = settingsStore.readingLetterSpacing

    /** How far apart a content page sets its lines, as a multiple of each style's own line height. */
    val readingLineHeight: StateFlow<Float> = settingsStore.readingLineHeight

    /** How much room a content page leaves at either side of its text. */
    val readingMargin: StateFlow<ReadingMargin> = settingsStore.readingMargin

    /**
     * Stores whether a content page draws its reading progress bar.
     *
     * @param enabled True draws the bar.
     */
    fun setReadingProgress(enabled: Boolean) = settingsStore.setReadingProgress(enabled)

    /**
     * Stores whether a content page's bars leave with the scroll.
     *
     * @param enabled True lets the finger put them away.
     */
    fun setBarsFollowScroll(enabled: Boolean) = settingsStore.setBarsFollowScroll(enabled)

    /**
     * Stores how large a content page's text draws.
     *
     * @param scale The size to draw at, as a multiple of the system's own.
     */
    fun setReadingTextScale(scale: Float) = settingsStore.setReadingTextScale(scale)

    /**
     * Stores how far apart a content page sets its characters.
     *
     * @param spacing The room to add, in sp, on top of the spacing prose is already set with.
     */
    fun setReadingLetterSpacing(spacing: Float) = settingsStore.setReadingLetterSpacing(spacing)

    /**
     * Stores how far apart a content page sets its lines.
     *
     * @param scale The multiple of each style's own line height.
     */
    fun setReadingLineHeight(scale: Float) = settingsStore.setReadingLineHeight(scale)

    /**
     * Stores how much room a content page leaves at either side of its text.
     *
     * @param margin The room to leave.
     */
    fun setReadingMargin(margin: ReadingMargin) = settingsStore.setReadingMargin(margin)
}
