package com.prslc.zhiflow.ui.page.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.disk.DiskCache
import com.prslc.zhiflow.core.preference.SettingsStore
import com.prslc.zhiflow.core.preference.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The settings page's own state: the theme in force, what the image cache holds, and emptying it.
 *
 * The cache size is read back from the disk cache Coil was built with rather than by walking the
 * directory, so the figure the row shows and what a clear removes are the same thing.
 *
 * @param imageLoader Coil's own instance, which is where that cache hangs. It is the loader the
 *   page is handed rather than a Context to reach one with, so nothing here holds a Context.
 * @param settingsStore Where the theme is kept. It publishes, which is how the Activity that
 *   draws the theme hears about a change made here.
 */
class SettingsViewModel(
    private val imageLoader: ImageLoader,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    /** The theme in force, emitting again whenever the reader picks another. */
    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode

    /** Bytes the image disk cache holds, or 0 until the first read lands. */
    var cacheSize by mutableLongStateOf(0L)
        private set

    /** True while a clear is in flight, so the row cannot start a second one. */
    var isClearing by mutableStateOf(false)
        private set

    /**
     * Stores the theme to draw in, which the Activity picks up and redraws in.
     *
     * @param mode The theme the reader picked.
     */
    fun setThemeMode(mode: ThemeMode) = settingsStore.setThemeMode(mode)

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
}
