package com.prslc.zhiflow.core.utils.platform

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch

/**
 * Returns a lambda that copies plain text to the system clipboard
 * using the coroutine-based [Clipboard] API, then confirms the write with
 * [HapticFeedbackType.Confirm].
 */
@SuppressLint("ComposeRedundantComposable")
@Composable
internal fun rememberCopyTextToClipboard(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, haptic) {
        { text ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(android.content.ClipData.newPlainText(null, text)))
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }
    }
}
