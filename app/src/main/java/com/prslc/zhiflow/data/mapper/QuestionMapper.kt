package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.model.AnswerTarget
import com.prslc.zhiflow.ui.component.common.ImageData

data class AnswerDisplay(
    val id: String,
    val authorName: String,
    val authorAvatar: String?,
    val excerpt: String,
    val images: List<ImageData>,
    val voteCount: Int,
    val commentCount: Int,
)

internal fun AnswerTarget.toDisplayData(): AnswerDisplay = AnswerDisplay(
    id = id,
    authorName = author.name ?: "",
    authorAvatar = author.avatar,
    excerpt = excerpt,
    images = (thumbnailInfo?.thumbnails ?: emptyList()).map {
        ImageData(url = it.url, width = it.width, height = it.height)
    },
    voteCount = voteupCount,
    commentCount = commentCount,
)
