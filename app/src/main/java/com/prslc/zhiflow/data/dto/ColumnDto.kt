package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

/** A column as its page draws it. */
@Immutable
data class ColumnDto(
    val urlToken: String,
    val title: String,
    val intro: String,
    val imageUrl: String?,
    val author: PersonDto,
    val followers: Int,
    val articlesCount: Int,
    val voteupCount: Int,
    val isFollowing: Boolean,
)

/** One row of a column's article list. */
@Immutable
data class ColumnItemDto(
    val id: String,
    val title: String,
    val excerpt: String,
    /** When it was published, in Unix seconds; 0 when the server did not say. */
    val created: Long,
    val imageUrl: String?,
    val voteupCount: Int,
    val commentCount: Int,
)
