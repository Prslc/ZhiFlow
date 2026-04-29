package com.prslc.zhiflow.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestionDetail(
    val id: Long,
    val title: String,
    val detail: String,
    val author: ZhihuUser,
    val topics: List<Topic> = emptyList(),
    @SerialName("answer_count") val answerCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("follower_count") val followerCount: Int = 0,
    @SerialName("updated_time") val updatedTime: Long = 0L
)

@Immutable
@Serializable
data class Topic(
    val id: String,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)