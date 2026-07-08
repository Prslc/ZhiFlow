package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.FeedDto
import com.prslc.zhiflow.data.model.feed.FeedItem
import com.prslc.zhiflow.ui.component.common.ImageData

internal fun FeedItem.toDto(): FeedDto? {
    val target = this.target ?: return null

    return FeedDto(
        id = target.id?.toString() ?: "",
        type = target.type ?: "answer",
        title = target.question?.title ?: target.title ?: "",
        authorName = target.author?.name ?: "",
        authorAvatar = target.author?.avatarUrl,
        excerpt = target.excerpt ?: "",
        images = target.thumbnails.map { ImageData(url = it, width = 0, height = 0) },
        voteCount = target.voteCount,
        commentCount = target.commentCount,
    )
}
