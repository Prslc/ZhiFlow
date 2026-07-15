package com.prslc.zhiflow.data.dto

/**
 * Flat, UI-ready representation of a content item within a user's collection.
 */
data class CollectionItemDto(
    val id: String,
    val type: String,
    val title: String,
    val authorName: String,
    val authorAvatar: String?,
    val excerpt: String,
    val thumbnail: String?,
    val voteCount: Int,
    val commentCount: Int,
    val collectTime: Long,
    val url: String,
    val collectionNames: List<String>,
)
