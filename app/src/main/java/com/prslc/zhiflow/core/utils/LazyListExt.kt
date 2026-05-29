package com.prslc.zhiflow.core.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf

/**
 * Returns a derived [State] that is `true` when the last visible item
 * is within 2 positions of the list end, signalling that more data
 * should be loaded for infinite scrolling.
 */
internal fun LazyListState.shouldLoadMore(): State<Boolean> = derivedStateOf {
    val layoutInfo = this.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val lastVisibleItemIndex = firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size
    totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 2
}
