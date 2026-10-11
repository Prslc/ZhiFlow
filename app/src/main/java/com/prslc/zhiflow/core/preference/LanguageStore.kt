package com.prslc.zhiflow.core.preference

/** Which language the app is written in. */
enum class AppLanguage {
    /** Whatever the system is set to. */
    System,

    /** Simplified Chinese. */
    Chinese,

    /** English. */
    English,
}

/**
 * Where the reader's language choice is kept.
 *
 * The platform keeps this one itself, through `LocaleManager`, so nothing is mirrored into our
 * own preferences: the choice is read back from the system, which is also what keeps the
 * system's own per-app language screen and our settings page agreeing with each other.
 *
 * A change is not published, because it does not need to be: the platform answers it by
 * recreating the Activity, and the new one reads the choice as it composes.
 */
interface LanguageStore {

    /** The language in force. */
    val language: AppLanguage

    /**
     * Sets the language, which the platform applies by recreating the Activity.
     *
     * @param language The language to write the app in.
     */
    fun setLanguage(language: AppLanguage)
}
