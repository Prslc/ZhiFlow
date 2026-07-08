package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.model.FeedItem
import com.prslc.zhiflow.ui.component.common.ImageData

data class FeedDisplay(
    val id: String,
    val type: String,
    val title: String,
    val authorName: String,
    val authorAvatar: String?,
    val excerpt: String,
    val images: List<ImageData>,
    val voteCount: Int,
    val commentCount: Int,
)

internal fun FeedItem.toDisplayData(): FeedDisplay? {
    val target = this.target ?: return null

    val type = target.type ?: "answer"
    val title = target.question?.title ?: target.title ?: ""

    return FeedDisplay(
        id = target.id?.toString() ?: "",
        type = type,
        title = title,
        authorName = target.author?.name ?: "",
        authorAvatar = target.author?.avatarUrl,
        excerpt = target.excerpt ?: "",
        images = target.thumbnails.map { ImageData(url = it, width = 0, height = 0) },
        voteCount = target.voteCount,
        commentCount = target.commentCount,
    )
}
