package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.AnswerDto
import com.prslc.zhiflow.data.model.feed.AnswerTarget
import com.prslc.zhiflow.ui.component.common.ImageData

internal fun AnswerTarget.toDto(): AnswerDto = AnswerDto(
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
