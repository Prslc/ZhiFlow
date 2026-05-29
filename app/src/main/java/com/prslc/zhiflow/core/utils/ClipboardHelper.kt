package com.prslc.zhiflow.core.utils

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

/**
 * Returns a lambda that copies plain text to the system clipboard
 * using the coroutine-based [Clipboard] API.
 */
@SuppressLint("ComposeRedundantComposable")
@Composable
internal fun rememberCopyTextToClipboard(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard) {
        { text ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(android.content.ClipData.newPlainText(null, text)))
            }
        }
    }
}
