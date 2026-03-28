package com.prslc.zhiflow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZhihuResponse(
    val data: List<FeedItem> = emptyList(),
    val paging: PagingData = PagingData()
)

@Serializable
data class PagingData(
    @SerialName("is_end") val isEnd: Boolean = false,
    val next: String? = null,    // next page
    val previous: String? = null // pre page
)

@Serializable
data class FeedItem(
    val target: FeedTarget? = null,
    val type: String? = null
)

@Serializable
data class FeedTarget(
    val id: Long? = 0,
    val type: String? = null,            // type
    val title: String? = null,           // title
    @SerialName("question") val question: Question? = null, // question
    val author: FeedAuthor? = null,      // author
    val excerpt: String? = null,         // excerpt
    val content: String? = null,         // content text
    @SerialName("voteup_count") val voteCount: Int = 0,     // Agree count
    @SerialName("comment_count") val commentCount: Int = 0  // comment count
)

@Serializable
data class Question(val title: String? = null)

@Serializable
data class FeedAuthor(
    val name: String = "Anonymous user",
    @SerialName("avatar_url") val avatarUrl: String? = null
)