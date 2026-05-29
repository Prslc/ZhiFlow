package com.prslc.zhiflow.core.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.text.TextLayoutResult

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