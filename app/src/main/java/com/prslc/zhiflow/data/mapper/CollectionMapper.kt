package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.CollectionItemDto
import com.prslc.zhiflow.data.model.user.CollectionContentItem

internal fun CollectionContentItem.toDto(): CollectionItemDto = CollectionItemDto(
    id = id.toString(),
    type = type,
    title = title
        .ifBlank { content.firstOrNull { it.type == "text" }?.title.orEmpty() }
        .ifBlank { question?.title.orEmpty() },
    authorName = author?.name ?: "",
    authorAvatar = author?.avatar,
    excerpt = excerpt.ifBlank { excerptTitle },
    thumbnail = thumbnail.takeIf { it.isNotBlank() }
        ?: imageUrl.takeIf { it.isNotBlank() }
        ?: content.firstOrNull { it.type == "image" }?.url?.takeIf { it.isNotBlank() }
        ?: cover?.coverUrl?.takeIf { it.isNotBlank() },
    voteCount = if (type.lowercase() == "pin") likeCount else voteupCount,
    commentCount = commentCount,
    collectTime = collectTime,
    url = url,
    collectionNames = collections.map { it.title },
)
