package com.prslc.zhiflow.ui.page.profile

import androidx.lifecycle.ViewModel
import com.prslc.zhiflow.core.preference.ReadingMargin
import com.prslc.zhiflow.core.preference.ReadingTextSize
import com.prslc.zhiflow.core.preference.SettingsStore
import kotlinx.coroutines.flow.StateFlow

/**
 * The reading settings page: what a content page draws, and whether its bars step aside.
 *
 * Both choices are published by the store, which is what lets a change here reach a page already
 * open behind this one.
 *
 * @param settingsStore Where the two choices are kept.
 */
class ReadingSettingsViewModel(private val settingsStore: SettingsStore) : ViewModel() {

    /** Whether a content page draws its reading progress bar. */
    val readingProgress: StateFlow<Boolean> = settingsStore.readingProgress

    /** Whether a content page's bars leave with the scroll. */
    val barsFollowScroll: StateFlow<Boolean> = settingsStore.barsFollowScroll

    /** How large a content page's text draws. */
    val readingTextSize: StateFlow<ReadingTextSize> = settingsStore.readingTextSize

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
     * @param size The size to draw at.
     */
    fun setReadingTextSize(size: ReadingTextSize) = settingsStore.setReadingTextSize(size)

    /**
     * Stores how much room a content page leaves at either side of its text.
     *
     * @param margin The room to leave.
     */
    fun setReadingMargin(margin: ReadingMargin) = settingsStore.setReadingMargin(margin)
}
