package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.AnswerDto
import com.prslc.zhiflow.data.dto.ImageData
import com.prslc.zhiflow.data.dto.QuestionAuthorDto
import com.prslc.zhiflow.data.model.feed.AnswerTarget
import com.prslc.zhiflow.data.model.user.ZhihuUser

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

/** @return The asker, addressed the way a profile page reads an account. */
internal fun ZhihuUser.toQuestionAuthor(): QuestionAuthorDto = QuestionAuthorDto(
    profileToken = urlToken.ifBlank { id },
    name = name.orEmpty(),
    headline = headline.orEmpty(),
    avatarUrl = avatar.takeIf { !it.isNullOrBlank() },
)
