package com.prslc.zhiflow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollectionResponse(
    val data: List<ZhihuCollection>,
    val paging: Paging? = null,
    val totals: Int = 0
)

@Serializable
data class ZhihuCollection(
    val id: Long,
    val title: String,
    val description: String? = "",
    @SerialName("is_favorited") val isFavorited: Boolean,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("item_count") val itemCount: Int,
    @SerialName("updated_time") val updatedTime: Long,
    @SerialName("is_default") val isDefault: Boolean = false,
    val creator: AnswerAuthor? = null
)

@Serializable
data class Paging(
    @SerialName("is_end") val isEnd: Boolean = false,
    @SerialName("is_start") val isStart: Boolean = false,
    val next: String? = null,
    val previous: String? = null,
    val totals: Int = 0
)