package com.prslc.zhiflow.data.model.user

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Paged response from `/people/{uid}/collection_contents`.
 */
@Immutable
@Serializable
data class CollectionContentsResponse(
    val data: List<CollectionContentItem> = emptyList(),
    val paging: Paging? = null,
)

/**
 * A single content item (typically an answer) saved in a user's collection.
 */
@Immutable
@Serializable
data class CollectionContentItem(
    val id: Long = 0,
    val type: String = "",
    val title: String = "",
    val excerpt: String = "",
    val thumbnail: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("voteup_count") val voteupCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("collect_time") val collectTime: Long = 0,
    val url: String = "",
    val author: ZhihuUser? = null,
    val question: CollectionQuestion? = null,
    val collections: List<CollectionInfo> = emptyList(),
)

/**
 * Extracts title and ID from the nested `question` object within a collection content item.
 *
 * A dedicated type is used here rather than reusing [ZhihuQuestion]
 * because the latter carries mandatory non-default fields (e.g. `id`) that are absent in
 * some API responses, which would cause deserialization failures.
 */
@Immutable
@Serializable
data class CollectionQuestion(
    val id: Long = 0,
    val title: String = "",
)

/**
 * Lightweight collection-folder reference from the `collections[]` array of a content item.
 *
 * Does NOT reuse [ZhihuCollection] because that type requires a mandatory `isFavorited`
 * field which is absent from the nested reference objects in this endpoint.
 */
@Immutable
@Serializable
data class CollectionInfo(
    val id: Long = 0,
    val title: String = "",
)
