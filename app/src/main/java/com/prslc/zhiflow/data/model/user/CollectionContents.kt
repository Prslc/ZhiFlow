package com.prslc.zhiflow.data.model.user

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.Paging
import com.prslc.zhiflow.data.model.content.ZhihuQuestion
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
 * A single content item (an answer, an article or a pin) saved in a user's collection.
 *
 * The three types do not share a shape. Answers and articles carry `title`, `excerpt` and
 * `voteup_count`; a pin carries none of those — its title rides on its first text segment, its
 * votes in `like_count`, and its picture either in `content[]` or in `cover`. Its `excerpt_title`
 * is a one-line summary the backend truncates, not the body.
 */
@Immutable
@Serializable
data class CollectionContentItem(
    val id: Long = 0,
    val type: String = "",
    val title: String = "",
    val excerpt: String = "",
    @SerialName("excerpt_title") val excerptTitle: String = "",
    val thumbnail: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val cover: CollectionCover? = null,
    val content: List<CollectionPinContent> = emptyList(),
    @SerialName("voteup_count") val voteupCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("collect_time") val collectTime: Long = 0,
    val url: String = "",
    val author: ZhihuUser? = null,
    val question: CollectionQuestion? = null,
    val collections: List<CollectionInfo> = emptyList(),
)

/**
 * One segment of a collected pin's `content[]` array.
 *
 * Only what the list row draws is modeled: a pin's title lives on its text segment, and its
 * picture on an image one.
 */
@Immutable
@Serializable
data class CollectionPinContent(
    val type: String = "",
    val title: String = "",
    val url: String = "",
)

/**
 * Cover reference from the `cover` object of a collected pin.
 */
@Immutable
@Serializable
data class CollectionCover(
    @SerialName("cover_url") val coverUrl: String = "",
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
