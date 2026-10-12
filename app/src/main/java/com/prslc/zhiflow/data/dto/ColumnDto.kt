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
    val viewsCount: Int,
    val voteupCount: Int,
    val isFollowing: Boolean,
    /**
     * The author's standing: how many times they have been agreed with, liked and favorited, or
     * all three null where the server's words for it could not be read.
     */
    val authorAgree: Int?,
    val authorLike: Int?,
    val authorFavorite: Int?,
    /** The server's own words for it, drawn as they came when the three above are null. */
    val authorInfluenceWords: List<String>,
)

/** One row of a column's list. */
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
    val type: String,
)
