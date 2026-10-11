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
     * Stores the chosen theme, which the next composition draws in.
     *
     * @param mode The theme to draw in.
     */
    fun setThemeMode(mode: ThemeMode)

    /**
     * Stores whether to save data by leaving images unloaded.
     *
     * @param enabled True leaves each illustration and thumbnail a placeholder of its own size,
     *   which still opens when tapped. Formulas, avatars and covers are not images a reader pays
     *   traffic for, and are unaffected.
     */
    fun setDataSaver(enabled: Boolean)
}
