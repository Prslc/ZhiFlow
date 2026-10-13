package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

@Immutable
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
    // column fields: how many pieces a column holds. Its entry has no body of its own, so this
    // and the author are what its line is made of
    val contentCount: Int,
    // video fields: a video's line counts plays where an answer's counts comments
    val playCount: Int,
    // common
    val contentToken: String?,
    val readTime: Long,
)
