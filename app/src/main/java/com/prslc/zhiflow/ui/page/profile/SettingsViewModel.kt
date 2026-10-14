package com.prslc.zhiflow.ui.page.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.disk.DiskCache
import com.prslc.zhiflow.core.exception.ignoreOutcome
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.core.network.Credentials
import com.prslc.zhiflow.core.preference.AppLanguage
import com.prslc.zhiflow.core.preference.LanguageStore
import com.prslc.zhiflow.core.preference.SettingsStore
import com.prslc.zhiflow.core.preference.ThemeMode
import com.prslc.zhiflow.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The settings page's own state: the appearance in force, what the image cache holds, and
 * emptying it.
 *
 * The cache size is read back from the disk cache Coil was built with rather than by walking the
 * directory, so the figure the row shows and what a clear removes are the same thing.
 *
 * @param imageLoader Coil's own instance, which is where that cache hangs. It is the loader the
 *   page is handed rather than a Context to reach one with, so nothing here holds a Context.
 * @param settingsStore Where the theme is kept. It publishes, which is how the Activity that
 *   draws the theme hears about a change made here.
 * @param languageStore Where the language is kept, which is the platform's own record rather than
 *   ours. Nothing has to hear about a change: the platform recreates the Activity for it.
 * @param credentialStore Where the session is kept, read for whether anyone is signed in.
 * @param authRepository What the sign-out is done through.
 */
class SettingsViewModel(
    private val imageLoader: ImageLoader,
    private val settingsStore: SettingsStore,
    private val languageStore: LanguageStore,
    private val credentialStore: CredentialStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** The theme in force, emitting again whenever the reader picks another. */
    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode

    /** The language in force. Read straight through, since a change rebuilds this page. */
    val language: AppLanguage
        get() = languageStore.language

    /** Whether the reader asked to save data, emitting again whenever they change it. */
    val dataSaver: StateFlow<Boolean> = settingsStore.dataSaver

    /** Whether the developer tools are unlocked, which the about page is what unlocks. */
    val developerUnlocked: StateFlow<Boolean> = settingsStore.developerUnlocked

    /**
     * The session behind this page, emitting again whenever it changes.
     *
     * The account group is drawn from it: a reader with only an anonymous session behind them has
     * none to end.
     */
    val session: StateFlow<Credentials> = credentialStore.session

    /** Bytes the image disk cache holds, or 0 until the first read lands. */
    var cacheSize by mutableLongStateOf(0L)
        private set

    /** True while a clear is in flight, so the row cannot start a second one. */
    var isClearing by mutableStateOf(false)
        private set

    /** Set once the session has been dropped, which is what the page leaves on. */
    var signedOut by mutableStateOf(false)
        private set

    /**
     * Stores the theme to draw in, which the Activity picks up and redraws in.
     *
     * @param mode The theme the reader picked.
     */
    fun setThemeMode(mode: ThemeMode) = settingsStore.setThemeMode(mode)

    /**
     * Stores the language to write in. The platform recreates the Activity to apply it.
     *
     * @param language The language the reader picked.
     */
    fun setLanguage(language: AppLanguage) = languageStore.setLanguage(language)

    /**
     * Stores whether to save data by leaving images unloaded.
     *
     * @param enabled True leaves each of them a placeholder of its own size, which still opens.
     */
    fun setDataSaver(enabled: Boolean) = settingsStore.setDataSaver(enabled)

    /**
     * Reads the disk cache's current size, off the main thread — Coil answers it by walking its
     * own directory, and the page is only waiting to draw the number.
     */
    fun loadCacheSize() {
        viewModelScope.launch {
            cacheSize = withContext(Dispatchers.IO) { diskCache?.size ?: 0L }
        }
    }

    /**
     * Empties the image disk cache and re-reads its size, which is what the row then shows.
     *
     * The memory cache is deliberately left alone: the images on screen were decoded from it, and
     * dropping it would only make them decode again for a saving the system reclaims by itself.
     * No-op while a clear is already running.
     */
    fun clearCache() {
        if (isClearing) return
        isClearing = true
        viewModelScope.launch {
            cacheSize = withContext(Dispatchers.IO) {
                diskCache?.clear()
                diskCache?.size ?: 0L
            }
            isClearing = false
        }
    }

    private val diskCache: DiskCache?
        get() = imageLoader.diskCache

    /**
     * Ends the session, here and on the server.
     *
     * [signedOut] is what the page waits on rather than a return value: the work runs on this
     * ViewModel's scope, and a page that left before it finished would take that scope with it.
     * The store is cleared after the server has been told, so leaving early would be leaving
     * signed in.
     *
     * A server that could not be told is not worth reporting: the session is gone from this
     * device either way, and the guest bootstrap underneath replaces the cookie that would
     * otherwise have outlived it.
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.logout().ignoreOutcome()
            signedOut = true
        }
    }
}
