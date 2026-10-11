package com.prslc.zhiflow.core.preference

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/**
 * [LanguageStore] over the platform's own per-app language.
 *
 * @param context Used to reach [LocaleManager]; Koin supplies the application's own.
 */
class LanguageStoreImpl(private val context: Context) : LanguageStore {

    override val language: AppLanguage
        get() = when (localeManager?.applicationLocales?.toLanguageTags()) {
            null, "" -> AppLanguage.System
            CHINESE_TAG -> AppLanguage.Chinese
            ENGLISH_TAG -> AppLanguage.English
            else -> AppLanguage.System
        }

    override fun setLanguage(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.System -> LocaleList.getEmptyLocaleList()
            AppLanguage.Chinese -> LocaleList.forLanguageTags(CHINESE_TAG)
            AppLanguage.English -> LocaleList.forLanguageTags(ENGLISH_TAG)
        }
        localeManager?.applicationLocales = locales
    }

    /** Null where the platform has no such service, and following the system is the only answer. */
    private val localeManager: LocaleManager?
        get() = context.getSystemService(LocaleManager::class.java)

    private companion object {
        const val CHINESE_TAG = "zh-CN"
        const val ENGLISH_TAG = "en"
    }
}
