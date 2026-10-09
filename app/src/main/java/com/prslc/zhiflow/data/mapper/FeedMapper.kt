package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.FeedDto
import com.prslc.zhiflow.data.dto.FeedReason
import com.prslc.zhiflow.data.model.feed.CardChild
import com.prslc.zhiflow.data.model.feed.CardElement
import com.prslc.zhiflow.data.model.feed.CardImage
import com.prslc.zhiflow.data.model.feed.CardStyle
import com.prslc.zhiflow.data.model.feed.ComponentCard
import com.prslc.zhiflow.ui.component.common.ImageData
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.math.roundToInt

internal fun ComponentCard.toDto(styles: Map<String, CardStyle>): FeedDto? {
    val contentId = extra.contentId ?: return null

    return FeedDto(
        id = contentId,
        type = extra.contentType ?: "answer",
        title = children.titleText(),
        reason = children.reason(),
        authorName = children.authorName(),
        authorAvatar = children.authorAvatar(),
        authorNote = children.authorNote(),
        excerpt = children.summaryText(),
        images = children.images().map { image ->
            val ratio = image.style?.let(styles::get)?.scaleRatio
            ImageData(
                url = image.url ?: "",
                width = BOX_SCALE,
                height = ratio?.let { (BOX_SCALE / it).roundToInt() } ?: BOX_SCALE,
            )
        },
        voteCount = children.reactionCount("Vote"),
        commentCount = children.reactionCount("Comment"),
    )
}

/** An image's box arrives as a proportion, so one side of the pair carries a fixed scale. */
private const val BOX_SCALE = 1000

private const val TITLE_STYLE = "text_recommend_title"

private fun JsonElement?.textValue(): String =
    (this as? JsonPrimitive)?.contentOrNull.orEmpty()

private fun CardChild.isTitle(): Boolean = type == "Text" && style == TITLE_STYLE

private fun List<CardChild>.titleText(): String =
    firstOrNull { it.isTitle() }?.text.textValue()

/** The reason is the Button the card puts above its title, whatever the button style is called. */
private fun List<CardChild>.reason(): FeedReason? {
    val button = takeWhile { !it.isTitle() }.firstOrNull { it.type == "Button" } ?: return null
    val text = button.text.reasonText()
    if (text.isEmpty()) return null

    return FeedReason(
        text = text,
        iconUrl = button.icon?.url,
    )
}

/** A Button nests its sentence in a Text object; every other child carries the string itself. */
private fun JsonElement?.reasonText(): String =
    if (this is JsonObject) get("text").textValue() else textValue()

private fun List<CardChild>.summaryText(): String =
    firstOrNull { it.type == "Text" && it.id == "text_pin_summary" }?.text.textValue()

private fun List<CardChild>.authorRow(): List<CardElement> =
    firstOrNull { line ->
        line.elements.any { it.type == "Avatar" } && line.elements.any { it.type == "Text" }
    }?.elements.orEmpty()

/** The author's name, then whatever the card adds about them. */
private fun List<CardChild>.authorTexts(): List<String> =
    authorRow().filter { it.type == "Text" }.map { it.text.textValue() }

private fun List<CardChild>.authorName(): String =
    authorTexts().firstOrNull().orEmpty()

private fun List<CardChild>.authorAvatar(): String? =
    authorRow().firstOrNull { it.type == "Avatar" }?.image?.url

/** The note's style id is minted per card, so it is found by its place after the name instead. */
private fun List<CardChild>.authorNote(): String? =
    authorTexts().getOrNull(1)?.takeIf { it.isNotEmpty() }

private fun List<CardChild>.images(): List<CardImage> =
    firstOrNull { it.type == "Images" }?.images.orEmpty()
        .filter { !it.url.isNullOrEmpty() }

private fun List<CardChild>.reactionCount(reaction: String): Int =
    flatMap { it.elements }
        .firstOrNull { it.reaction == reaction }?.count ?: 0
