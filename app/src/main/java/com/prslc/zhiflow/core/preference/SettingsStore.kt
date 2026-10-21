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
 * The choices each reading setting offers, in the order the page draws them.
 *
 * The text size is a multiple rather than a size: a reader who has asked the system for larger text
 * keeps that choice, and this moves from there instead of overruling it. The letter spacing is
 * added in sp to the spacing prose is already set with, and the line height is a multiple of the
 * line height each style gives itself, so neither has to be re-chosen when the text size moves.
 */
object ReadingChoices {
    /** Text size, as a multiple of the size the system is set to. */
    val textScales = listOf(0.85f, 0.9f, 1f, 1.1f, 1.2f, 1.3f)

    /** Letter spacing, in sp added on top of the spacing prose is already set with. */
    val letterSpacings = listOf(-0.2f, 0f, 0.2f, 0.4f, 0.6f, 0.8f)

    /** Line height, as a multiple of the line height a style gives itself. */
    val lineHeights = listOf(1f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f)

    /**
     * The room a content page leaves at either side, in dp.
     *
     * Named for the room rather than for the line, because the room is what the reader is
     * changing: a narrow margin leaves a longer line, and it is the margin they see on the page.
     */
    val margins = listOf(12f, 14f, 16f, 18f, 20f, 22f, 24f, 26f, 28f, 30f, 32f)
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
     * How large a content page's text draws, as a multiple of the size the system is set to.
     *
     * Emitting again whenever it changes: the page may already be open behind the settings, and it
     * draws its text from this rather than from a value read once.
     */
    val readingTextScale: StateFlow<Float>

    /**
     * How far apart a content page sets its characters, in sp added to what prose already uses.
     *
     * Emitting again whenever it changes, for the same reason: the page may already be open.
     */
    val readingLetterSpacing: StateFlow<Float>

    /**
     * How far apart a content page sets its lines, as a multiple of each style's own line height.
     *
     * Emitting again whenever it changes, for the same reason: the page may already be open.
     */
    val readingLineHeight: StateFlow<Float>

    /**
     * How much room a content page leaves at either side of its text, in dp.
     *
     * Emitting again whenever it changes, for the same reason: the page may already be open.
     */
    val readingMargin: StateFlow<Float>

    /**
     * Whether a content page marks the passages that carry comments.
     *
     * Emitting again whenever it changes: the page may already be open behind the settings, and the
     * marks are drawn from this rather than from a value read once when the page was.
     */
    val segmentComments: StateFlow<Boolean>

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
     * Stores how large a content page's text draws.
     *
     * @param scale The size to draw at, as a multiple of the system's own.
     */
    fun setReadingTextScale(scale: Float)

    /**
     * Stores how far apart a content page sets its characters.
     *
     * @param spacing The room to add, in sp, on top of the spacing prose is already set with.
     */
    fun setReadingLetterSpacing(spacing: Float)

    /**
     * Stores how far apart a content page sets its lines.
     *
     * @param scale The multiple of each style's own line height.
     */
    fun setReadingLineHeight(scale: Float)

    /**
     * Stores how much room a content page leaves at either side of its text.
     *
     * @param margin The room to leave, in dp.
     */
    fun setReadingMargin(margin: Float)

    /**
     * Stores whether a content page marks the passages that carry comments.
     *
     * @param enabled False draws the text unmarked, and takes the way into a passage's comments
     *   with the marks, which are the only place they open from.
     */
    fun setSegmentComments(enabled: Boolean)

    /**
     * Stores whether the developer tools are unlocked, which is what the settings page offers
     * them on.
     *
     * @param unlocked False hides them again, and with them the way back in until the version
     *   number is tapped out once more.
     */
    fun setDeveloperUnlocked(unlocked: Boolean)
}
