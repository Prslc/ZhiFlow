package com.prslc.zhiflow.ui.page.feed

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * What the feed is told when a card it is drawing is blocked on another page.
 *
 * The block-list page is a destination of its own, so it cannot reach the feed's ViewModel.
 */
class BlockedContent {

    private val _blocked = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** The ids of cards blocked elsewhere, in the order they were blocked. */
    val blocked: SharedFlow<String> = _blocked.asSharedFlow()

    fun block(contentId: String) {
        _blocked.tryEmit(contentId)
    }
}
