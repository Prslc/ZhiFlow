package com.prslc.zhiflow.parser.emoji

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

object EmojiParser {
    private val emojiRegex = Regex("""\[[^\]]+]""")

    fun parse(text: String): AnnotatedString {
        return buildAnnotatedString {
            var lastIndex = 0

            emojiRegex.findAll(text).forEach { match ->
                append(text.substring(lastIndex, match.range.first))

                val tag = match.value
                val fileName = EmojiMap.tagToFileName[tag]

                if (fileName != null) {
                    val currentPosition = length
                    val inlineId = "emoji_${currentPosition}_${tag.hashCode()}"

                    addStringAnnotation(
                        tag = "EMOJI_ID",
                        annotation = inlineId,
                        start = currentPosition,
                        end = currentPosition + 1
                    )
                    addStringAnnotation(
                        tag = "EMOJI_PATH",
                        annotation = EmojiMap.getFullUrl(fileName),
                        start = currentPosition,
                        end = currentPosition + 1
                    )

                    appendInlineContent(inlineId, tag)
                } else {
                    append(tag)
                }
                lastIndex = match.range.last + 1
            }
            append(text.substring(lastIndex))
        }
    }
}