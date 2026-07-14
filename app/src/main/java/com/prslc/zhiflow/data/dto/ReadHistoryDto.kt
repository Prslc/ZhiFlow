package com.prslc.zhiflow.data.dto

data class ReadHistoryDto(
    val questionTitle: String,
    val questionToken: String?,
    val contentTypeIcon: String?,
    val contentType: String?,
    // answer fields
    val authorName: String?,
    val summary: String?,
    val coverImage: String?,
    val voteCount: Int,
    val commentCount: Int,
    val readProgress: Int,
    // question fields
    val answerCount: Int,
    val followerCount: Int,
    // common
    val contentToken: String?,
    val readTime: Long,
)
