package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.ui.component.common.ImageData

@Immutable
data class FeedDto(
    val id: String,
    val type: String,
    val title: String,
    val reason: FeedReason?,
    val authorName: String,
    val authorAvatar: String?,
    val authorNote: FeedAuthorNote?,
    val excerpt: String,
    val images: List<ImageData>,
    val voteCount: Int,
    val commentCount: Int,
)

@Immutable
data class FeedReason(
    val text: String,
    val iconUrl: String?,
)

/**
 * The chip a card puts after the author's name, saying what the reader is to them.
 *
 * [kind] and [count] are read out of the [text] and the `test_id` beside it; the sentence is kept
 * as it came, and is what is drawn for a kind this app does not know.
 */
@Immutable
data class FeedAuthorNote(
    val text: String,
    val kind: String?,
    val count: Int?,
)
