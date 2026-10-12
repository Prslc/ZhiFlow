package com.prslc.zhiflow.data.model.column

import com.prslc.zhiflow.data.model.user.ZhihuPerson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A column: what its own page is headed with.
 *
 * A column carries two identifiers, a numeric [id] and the [urlToken] every route into it uses;
 * only the token is read here.
 *
 * `followers` is the count to draw. The response also carries a `follower_count`, which is a
 * leftover zero — that one is the field name people objects use for the same thing.
 */
@Serializable
data class ZhihuColumn(
    @SerialName("url_token") val urlToken: String = "",
    val title: String = "",
    val intro: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val author: ZhihuPerson = ZhihuPerson(),
    @SerialName("author_ext") val authorExt: ColumnAuthorExt? = null,
    val followers: Int = 0,
    @SerialName("articles_count") val articlesCount: Int = 0,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("voteup_count") val voteupCount: Int = 0,
    @SerialName("is_following") val isFollowing: Boolean = false,
)

/**
 * A page of a column's list: the articles it published, and the answers it collects.
 *
 * The paging here is an explicit `limit`/`offset` rather than the cursor most of the app follows,
 * and the cursor it does hand back names another host over plain http; the repository pages this
 * one by offset instead.
 */
@Serializable
data class ZhihuColumnItems(
    val data: List<ZhihuColumnItem> = emptyList(),
    val paging: ZhihuColumnPaging = ZhihuColumnPaging(),
)

@Serializable
data class ZhihuColumnPaging(
    @SerialName("is_end") val isEnd: Boolean = false,
    val totals: Int = 0,
)

/**
 * The question an answer belongs to; the row is headed with its title.
 *
 * Its own type rather than [ZhihuQuestion], whose `id` and `title` are mandatory: a question
 * object missing either would fail the whole page's decoding.
 */
@Serializable
data class ColumnItemQuestion(val title: String = "")

/**
 * The author's standing, worded: `["6046 次赞同", "3886 次喜欢", "4098 次收藏"]`.
 *
 * The response carries an `author_ext` in two places, and this is the outer one; the one inside
 * `author` is an empty object.
 */
@Serializable
data class ColumnAuthorExt(@SerialName("influence_desc") val influenceDesc: List<String> = emptyList())

/** One entry of a column's list — an article, or an answer with the question it was written for. */
@Serializable
data class ZhihuColumnItem(
    val id: String = "",
    val title: String = "",
    val excerpt: String = "",
    val created: Long = 0,
    @SerialName("created_time") val createdTime: Long = 0,
    @SerialName("image_url") val imageUrl: String = "",
    val thumbnail: String = "",
    val question: ColumnItemQuestion? = null,
    @SerialName("voteup_count") val voteupCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("type") val type: String = "",
)
