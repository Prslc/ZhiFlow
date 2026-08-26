package com.prslc.zhiflow.ui.component.common

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
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
