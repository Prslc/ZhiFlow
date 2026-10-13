package com.prslc.zhiflow.core.utils.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf

/**
 * Returns a derived [State] that is `true` when the last visible item
 * is within 2 positions of the list end, signaling that more data
 * should be loaded for infinite scrolling.
 */
internal fun LazyListState.shouldLoadMore(): State<Boolean> = derivedStateOf {
    val layoutInfo = this.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val lastVisibleItemIndex = firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size
    totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 2
}

/**
 * The list a page of a paged response leaves behind, in the one shape every paged state is built
 * with.
 *
 * Wherever a list is drawn under a key taken from the row itself -- `key = { it.id }` -- a key
 * measured twice throws: `SubcomposeLayout` keeps its slots by that key, and the second row with
 * one key finds the first one's slot. A page can carry a row the list already holds, because a
 * page is addressed either by a count the client keeps or by a cursor the server computes, and
 * either can land on an entry an earlier page handed over: the column's own order moves as its
 * author publishes, a follow list's as people come and go, and a cursor is only as still as the
 * order behind it.
 *
 * So the page is taken in here rather than at each call site, and what the list already holds is
 * dropped instead of appended beside it. Deciding per call site is what let the column ship
 * without the guard the feed had.
 *
 * @param page The rows the response carried, in the order it carried them.
 * @param firstPage True when this page replaces the list rather than following it. The list is
 *   then not consulted at all: what it held is off screen, and a row the page repeats is one row.
 * @param keyOf The key the rows are drawn under, which is the list's own `key` parameter.
 * @return The rows to hold, with no key appearing twice, and the order they arrived in.
 */
internal fun <T> List<T>.withPage(
    page: List<T>,
    firstPage: Boolean,
    keyOf: (T) -> Any?,
): List<T> {
    if (firstPage) return page.distinctBy(keyOf)

    val known = mapTo(HashSet(), keyOf)
    return this + page.filter { known.add(keyOf(it)) }
}

