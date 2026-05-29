package com.prslc.zhiflow.core.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextLayoutResult
import kotlinx.coroutines.launch

/**
 * Text overflow detection extension
 */
internal fun TextLayoutResult?.isOverflowed(): Boolean {
    if (this == null) return false
    return hasVisualOverflow || (lineCount > 0 && isLineEllipsized(lineCount - 1))
}

internal fun LazyListState.shouldLoadMore(): State<Boolean> = derivedStateOf {
    val layoutInfo = this.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val lastVisibleItemIndex = firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size
    totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 2
}

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
