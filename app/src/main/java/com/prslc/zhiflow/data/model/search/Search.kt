package com.prslc.zhiflow.data.model.search

import com.prslc.zhiflow.data.model.Paging
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZhihuSearchResponse(
    val data: List<ZhihuSearchCard> = emptyList(),
    val paging: Paging = Paging(),
    @SerialName("search_action_info") val searchActionInfo: ZhihuSearchActionInfo =
        ZhihuSearchActionInfo(),
)

/**
 * One card of a result page.
 *
 * `data[]` is not a list of one thing, and its `object` has a shape of its own per kind.
 * Every kind is decoded into [ZhihuSearchContent] anyway, which is safe rather than right:
 * every field has a default, so a kind this app does not draw arrives anyway, told apart from
 * the ones it does by [ZhihuSearchContent.type]. A `relevant_query` card has no `object` at all,
 * carrying its words in [queryList].
 */
@Serializable
data class ZhihuSearchCard(
    val type: String = "",
    val id: String = "",
    val index: Int = 0,
    @SerialName("object") val content: ZhihuSearchContent? = null,
    @SerialName("query_list") val queryList: List<ZhihuRelevantQuery> = emptyList(),
    @SerialName("is_single_column") val isSingleColumn: Boolean = false,
)

@Serializable
data class ZhihuSearchActionInfo(
    @SerialName("search_hash_id") val searchHashId: String = "",
    @SerialName("lc_idx") val lcIndex: Int = 0,
)

/**
 * The `object` of a `search_result` card.
 *
 * An article, an answer and a person all land here: the fields a result row is drawn from are the
 * ones the kinds share, and [type] is what tells them apart. A person is the one that carries
 * [name] and [headline] instead of a title, and an id that is a user hash rather than a content id.
 */
@Serializable
data class ZhihuSearchContent(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val excerpt: String = "",
    val name: String = "",
    val headline: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("url_token") val urlToken: String = "",
    @SerialName("created_time") val createdTime: Long = 0,
    @SerialName("voteup_count") val voteupCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("zfav_count") val zfavCount: Int = 0,
    val author: ZhihuSearchAuthor = ZhihuSearchAuthor(),
    @SerialName("thumbnail_info") val thumbnailInfo: ZhihuSearchThumbnails =
        ZhihuSearchThumbnails(),
    @SerialName("is_following") val isFollowing: Boolean = false,
)

@Serializable
data class ZhihuSearchAuthor(
    val id: String = "",
    @SerialName("url_token") val urlToken: String = "",
    val name: String = "",
    val headline: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("is_following") val isFollowing: Boolean = false,
)

@Serializable
data class ZhihuSearchThumbnails(
    val count: Int = 0,
    val thumbnails: List<ZhihuSearchThumbnail> = emptyList(),
)

@Serializable
data class ZhihuSearchThumbnail(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
data class ZhihuRelevantQuery(
    val query: String = "",
    val id: String = "",
    @SerialName("has_icon") val hasIcon: Boolean = false,
)

@Serializable
data class ZhihuSuggestResponse(
    val suggest: List<ZhihuSuggestItem> = emptyList(),
)

@Serializable
data class ZhihuSuggestItem(
    val query: String = "",
    val id: Long = 0,
)

@Serializable
data class ZhihuSuggestedQueryResponse(
    @SerialName("query_list") val queryList: List<ZhihuSuggestedQuery> = emptyList(),
)

/** [realQuery] is the whole phrase to search for; [query] is only the ending it adds. */
@Serializable
data class ZhihuSuggestedQuery(
    val query: String = "",
    @SerialName("real_query") val realQuery: String = "",
)

@Serializable
data class ZhihuPresetWordsResponse(
    @SerialName("preset_words") val presetWords: ZhihuPresetWords = ZhihuPresetWords(),
)

@Serializable
data class ZhihuPresetWords(
    val words: List<ZhihuPresetWord> = emptyList(),
)

@Serializable
data class ZhihuPresetWord(
    val query: String = "",
    @SerialName("real_query") val realQuery: String = "",
    val label: String = "",
)
