package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.ColumnDto
import com.prslc.zhiflow.data.dto.ColumnItemDto
import com.prslc.zhiflow.data.model.column.ZhihuColumn
import com.prslc.zhiflow.data.model.column.ZhihuColumnItem

/** @return The column as its page draws it. */
internal fun ZhihuColumn.toDto(): ColumnDto = ColumnDto(
    urlToken = urlToken,
    title = title,
    intro = intro,
    imageUrl = imageUrl.takeIf { it.isNotBlank() },
    author = author.toPerson(),
    followers = followers,
    viewsCount = viewsCount,
    voteupCount = voteupCount,
    isFollowing = isFollowing,
)

/** @return The entry as one row of the column's list. */
internal fun ZhihuColumnItem.toDto(): ColumnItemDto = ColumnItemDto(
    id = id,
    title = title.ifBlank { question?.title.orEmpty() },
    excerpt = excerpt,
    created = created.takeIf { it > 0 } ?: createdTime,
    imageUrl = thumbnail.takeIf { it.isNotBlank() } ?: imageUrl.takeIf { it.isNotBlank() },
    voteupCount = voteupCount,
    commentCount = commentCount,
    type = type,
)
