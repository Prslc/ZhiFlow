package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.FeedDto
import com.prslc.zhiflow.data.model.feed.CardChild
import com.prslc.zhiflow.data.model.feed.CardElement
import com.prslc.zhiflow.data.model.feed.CardImage
import com.prslc.zhiflow.data.model.feed.ComponentCard
import com.prslc.zhiflow.ui.component.common.ImageData
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun ComponentCard.toDto(): FeedDto? {
    val contentId = extra.contentId ?: return null

    return FeedDto(
        id = contentId,
        type = extra.contentType ?: "answer",
        title = children.titleText(),
        authorName = children.authorName(),
        authorAvatar = children.authorAvatar(),
        excerpt = children.summaryText(),
        images = children.images()
            .map { ImageData(url = it.url ?: "", width = 0, height = 0) },
        voteCount = children.reactionCount("Vote"),
        commentCount = children.reactionCount("Comment"),
    )
}

private fun JsonElement?.textValue(): String =
    (this as? JsonPrimitive)?.contentOrNull.orEmpty()

private fun List<CardChild>.titleText(): String =
    firstOrNull { it.type == "Text" && it.style == "text_recommend_title" }?.text.textValue()

private fun List<CardChild>.summaryText(): String =
    firstOrNull { it.type == "Text" && it.id == "text_pin_summary" }?.text.textValue()

private fun List<CardChild>.authorRow(): List<CardElement> =
    firstOrNull { line ->
        line.elements.any { it.type == "Avatar" } && line.elements.any { it.type == "Text" }
    }?.elements.orEmpty()

private fun List<CardChild>.authorName(): String =
    authorRow().firstOrNull { it.type == "Text" }?.text.textValue()

private fun List<CardChild>.authorAvatar(): String? =
    authorRow().firstOrNull { it.type == "Avatar" }?.image?.url

private fun List<CardChild>.images(): List<CardImage> =
    firstOrNull { it.type == "Images" }?.images.orEmpty()
        .filter { !it.url.isNullOrEmpty() }

private fun List<CardChild>.reactionCount(reaction: String): Int =
    flatMap { it.elements }
        .firstOrNull { it.reaction == reaction }?.count ?: 0
