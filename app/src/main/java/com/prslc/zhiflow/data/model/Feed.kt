package com.prslc.zhiflow.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ZhihuResponse(
    val data: List<FeedItem> = emptyList(),
    val paging: PagingData = PagingData()
)

@Serializable
data class PagingData(
    @SerialName("is_end") val isEnd: Boolean = false,
    val next: String? = null,    // 下一页的 URL
    val previous: String? = null // 上一页的 URL
)

@Serializable
data class FeedItem(
    val target: FeedTarget? = null,
    val type: String? = null
)

@Serializable
data class FeedTarget(
    val id: Long? = 0,
    val title: String? = null,           // 文章标题
    @SerialName("question") val question: Question? = null, // 回答所属的问题
    val author: Author? = null,
    val excerpt: String? = null,         // 摘要
    val content: String? = null,         // 那个巨大的 HTML 正文
    @SerialName("voteup_count") val voteCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0
)

@Serializable
data class Question(val title: String? = null)

@Serializable
data class Author(
    val name: String = "匿名用户",
    @SerialName("avatar_url") val avatarUrl: String? = null
)