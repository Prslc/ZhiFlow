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
    articlesCount = articlesCount,
    voteupCount = voteupCount,
    isFollowing = isFollowing,
)

/** @return The article as one row of the column's list. */
internal fun ZhihuColumnItem.toDto(): ColumnItemDto = ColumnItemDto(
    id = id,
    title = title,
    excerpt = excerpt,
    created = created,
    imageUrl = imageUrl.takeIf { it.isNotBlank() },
    voteupCount = voteupCount,
    commentCount = commentCount,
)
