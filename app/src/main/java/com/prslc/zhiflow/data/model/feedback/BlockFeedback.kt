package com.prslc.zhiflow.data.model.feedback

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal const val SCENE_RECOMMEND = "RECOMMEND"
internal const val DELIVER_NORMAL = "Normal"

/** Response of `GET /zrec-feedback/content-tags`. */
@Immutable
@Serializable
data class ContentTagList(
    val data: List<ContentTag> = emptyList(),
)

@Immutable
@Serializable
data class ContentTag(
    val id: Long = 0,
    @SerialName("val") val value: String = "",
)

/** Response of `GET /feed-root/block`: the keywords already blocked, plus their limits. */
@Immutable
@Serializable
data class FeedRootBlock(
    @SerialName("kw_min_length") val minLength: Int = 0,
    @SerialName("kw_max_length") val maxLength: Int = 0,
    @SerialName("kw_max_count") val maxCount: Int = 0,
    val data: List<String> = emptyList(),
)

/**
 * Body of `POST /zrec-feedback/content/sub/agg/feedback`, which carries the page's whole
 * state at once: `keywords` is the complete list to keep, not an increment.
 */
@Immutable
@Serializable
data class AggFeedbackRequest(
    @SerialName("content_type") val contentType: String,
    @SerialName("content_token") val contentToken: String,
    @SerialName("scene_code") val sceneCode: String,
    @SerialName("feed_deliver_type") val feedDeliverType: String,
    @SerialName("content_tags") val contentTags: List<AggFeedbackTag>,
    val keywords: List<String>,
)

@Immutable
@Serializable
data class AggFeedbackTag(
    val id: Long,
    @SerialName("val") val value: String,
    @SerialName("isSelected") val isSelected: Boolean,
)
