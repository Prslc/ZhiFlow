package com.prslc.zhiflow.ui.component.common

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat

/**
 * Drives status bar icon appearance while this composable is in the
 * composition, restoring the system default on disposal.
 *
 * @param darkIcons Whether status bar icons should be dark (use dark icons
 *   on light backgrounds behind the status bar; use light/white icons on
 *   dark content such as cover images).
 *
 * Only one instance should be active at a time: overlapping instances do
 * not re-assert after an earlier one is disposed.
*/
@Composable
fun StatusBarIconEffect(
    darkIcons: Boolean,
) {
    val view = LocalView.current
    val activityWindow = remember(view) { (view.context as? ComponentActivity)?.window }
    val defaultDarkIcons = !isSystemInDarkTheme()

    DisposableEffect(activityWindow, darkIcons) {
        activityWindow?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        }
        onDispose {
            activityWindow?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = defaultDarkIcons
            }
        }
    }
}

/**
 * Light status bar icons while a page's cover image is behind them, and the theme's own icons once
 * the page's surface has scrolled up to the bar.
 *
 * A cover can be any colour, so dark icons over one are a coin toss; the surface a page draws its
 * own, by contrast, is the theme's, and takes the theme's icons. A page whose top bar fades in as
 * the cover leaves does not need this -- past the point where the bar is opaque the icons sit on
 * the bar, not on the cover.
 *
 * @param scrollState The page's scroll, in pixels.
 * @param coverHeight How tall the cover is, measured from the top of the page, status bar included.
 */
@Composable
fun CoverStatusBarIconEffect(
    scrollState: ScrollState,
    coverHeight: Dp,
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // The icons stand in the middle of the bar rather than along its bottom edge, so what the cover
    // has to clear is that line.
    val clearance = with(LocalDensity.current) {
        (coverHeight - statusBarHeight / 2).roundToPx()
    }
    val pastCover by remember(scrollState, clearance) {
        derivedStateOf { scrollState.value >= clearance }
    }

    StatusBarIconEffect(darkIcons = pastCover && !isSystemInDarkTheme())
}
