package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.CollectionItemDto
import com.prslc.zhiflow.data.model.user.CollectionContentItem

internal fun CollectionContentItem.toDto(): CollectionItemDto = CollectionItemDto(
    id = id.toString(),
    type = type,
    title = title.ifBlank { question?.title.orEmpty() },
    authorName = author?.name ?: "",
    authorAvatar = author?.avatar,
    excerpt = excerpt,
    thumbnail = thumbnail.takeIf { it.isNotBlank() } ?: imageUrl.takeIf { it.isNotBlank() },
    voteCount = voteupCount,
    commentCount = commentCount,
    collectTime = collectTime,
    url = url,
    collectionNames = collections.map { it.title },
)
