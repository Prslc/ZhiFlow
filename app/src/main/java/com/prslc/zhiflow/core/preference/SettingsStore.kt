package com.prslc.zhiflow.core.preference

import kotlinx.coroutines.flow.StateFlow

/** Which theme the app draws in. */
enum class ThemeMode {
    /** Whatever the system is set to. */
    System,

    /** The light scheme, whatever the system is set to. */
    Light,

    /** The dark scheme, whatever the system is set to. */
    Dark,
}

/**
 * Where the reader's own settings are kept.
 *
 * A port over the same preferences file the credentials live in: writes go through it and survive
 * a sign-out, which removes only the keys it owns.
 *
 * Reads publish. A theme picked on the settings page has to reach the Activity that draws it, and
 * nothing in that Activity makes a request of its own that would come back and tell it.
 */
interface SettingsStore {

    /** The chosen theme, emitting again whenever it changes. */
    val themeMode: StateFlow<ThemeMode>

    /**
     * Whether the reader has asked to save data.
     *
     * Emitting again whenever it changes: a page of images may already be on screen, and the
     * choice has to reach it without a reload.
     */
    val dataSaver: StateFlow<Boolean>

    /**
     * Whether the search box offers the phrases the reader has searched for, in place of the hot
     * searches.
     *
     * Emitting again whenever it changes: the box draws what this names, and it may already be on
     * screen when the settings page behind it is changed.
     */
    val searchHistory: StateFlow<Boolean>

    /**
     * Whether a content page draws the bar along its top that shows how far it has been read.
     *
     * Emitting again whenever it changes: the page may already be open behind the settings, and
     * the bar is drawn from this rather than from a value read once when the page was.
     */
    val readingProgress: StateFlow<Boolean>

    /**
     * Whether a content page's top and bottom bars leave with the scroll and come back with it.
     *
     * Emitting again whenever it changes, for the same reason: the bars are under a finger on a
     * page that may already be open.
     */
    val barsFollowScroll: StateFlow<Boolean>

    /**
     * Stores the chosen theme, which the next composition draws in.
     *
     * @param mode The theme to draw in.
     */
    fun setThemeMode(mode: ThemeMode)

    /**
     * Whether the developer tools are unlocked.
     *
     * Off until the version number on the about page has been tapped enough times, which is how
     * the platform hides its own developer options and so what a reader looks for.
     */
    val developerUnlocked: StateFlow<Boolean>

    /**
     * Stores whether to save data by leaving images unloaded.
     *
     * @param enabled True leaves each illustration and thumbnail a placeholder of its own size,
     *   which still opens when tapped. Formulas, avatars and covers are not images a reader pays
     *   traffic for, and are unaffected.
     */
    fun setDataSaver(enabled: Boolean)

    /**
     * Stores whether the search box draws the reader's own phrases rather than the hot searches.
     *
     * @param enabled True draws what they have searched for. Recording carries on either way, so
     *   turning it back on offers what was searched for meanwhile.
     */
    fun setSearchHistory(enabled: Boolean)

    /**
     * Stores whether a content page draws its reading progress bar.
     *
     * @param enabled True draws the bar. The reading position is tracked and reported either way.
     */
    fun setReadingProgress(enabled: Boolean)

    /**
     * Stores whether a content page's bars leave with the scroll and come back with it.
     *
     * @param enabled True lets the finger put them away and bring them back.
     */
    fun setBarsFollowScroll(enabled: Boolean)

    /**
     * Stores whether the developer tools are unlocked, which is what the settings page offers
     * them on.
     *
     * @param unlocked False hides them again, and with them the way back in until the version
     *   number is tapped out once more.
     */
    fun setDeveloperUnlocked(unlocked: Boolean)
}
