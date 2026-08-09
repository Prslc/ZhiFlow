package com.prslc.zhiflow.data.model.feed

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Immutable
@Serializable
data class ZhihuResponse(
    val data: List<ComponentCard> = emptyList(),
    val paging: PagingData = PagingData(),
)

@Immutable
@Serializable
data class PagingData(
    @SerialName("is_end") val isEnd: Boolean = false,
    val next: String? = null,    // next page
    val previous: String? = null // previous page
)

@Immutable
@Serializable
data class ComponentCard(
    val id: String? = null,
    val type: String? = null,
    val style: String? = null,
    val action: CardAction? = null,
    val extra: CardExtra = CardExtra(),
    val children: List<CardChild> = emptyList(),
)

@Immutable
@Serializable
data class CardAction(
    val type: String? = null,
    val parameter: String? = null,
)

@Immutable
@Serializable
data class CardExtra(
    @SerialName("content_id") val contentId: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("test_id") val testId: String? = null,
)

@Immutable
@Serializable
data class CardChild(
    val id: String? = null,
    val type: String? = null,
    val style: String? = null,
    val text: JsonElement? = null,
    val visible: Boolean = true,
    val elements: List<CardElement> = emptyList(),
    val images: List<CardImage> = emptyList(),
    val image: CardImage? = null,
)

@Immutable
@Serializable
data class CardElement(
    val id: String? = null,
    val type: String? = null,
    val style: String? = null,
    val text: JsonElement? = null,
    val count: Int = 0,
    val reaction: String? = null,
    val image: CardImage? = null,
    val badge: CardImage? = null,
    val border: CardImage? = null,
)

@Immutable
@Serializable
data class CardImage(
    val id: String? = null,
    val type: String? = null,
    val url: String? = null,
    val token: String? = null,
    val width: Int = 0,
    val height: Int = 0,
)
