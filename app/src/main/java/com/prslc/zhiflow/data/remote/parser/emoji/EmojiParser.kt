package com.prslc.zhiflow.data.remote.parser.emoji

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder

/**
 * One bracket in a text that names an emoji this app carries.
 *
 * @param start Where the bracket opens, as an offset into the text it was found in.
 * @param end Just past its closing bracket.
 * @param tag The bracket itself, which is the text an unresolved emoji falls back to.
 * @param fileName The bundled asset that draws it.
 */
data class EmojiMatch(
    val start: Int,
    val end: Int,
    val tag: String,
    val fileName: String,
)

object EmojiParser {
    private val emojiRegex = Regex("""\[[^\]]+]""")

    /**
     * The emoji of [text], in the order they appear.
     *
     * A bracket that names nothing this app carries is not one: a body is as free to write
     * `[小程序]` as it is to write `[捂脸]`, and only the second is a picture.
     *
     * @param text The text to scan.
     */
    fun knownEmoji(text: String): List<EmojiMatch> = emojiRegex.findAll(text).mapNotNull { match ->
        val tag = match.value
        EmojiMap.tagToFileName[tag]?.let { fileName ->
            EmojiMatch(
                start = match.range.first,
                end = match.range.last + 1,
                tag = tag,
                fileName = fileName,
            )
        }
    }.toList()

    fun parse(text: String): AnnotatedString {
        return buildAnnotatedString {
            var lastIndex = 0

            emojiRegex.findAll(text).forEach { match ->
                append(text.substring(lastIndex, match.range.first))

                val tag = match.value
                val fileName = EmojiMap.tagToFileName[tag]

                if (fileName != null) {
                    val currentPosition = length
                    val inlineId = AnnotatedStringBuilder.emojiInlineId(currentPosition)

                    appendInlineContent(inlineId, tag)
                    addStringAnnotation(
                        tag = AnnotatedStringBuilder.EMOJI_ID_TAG,
                        annotation = inlineId,
                        start = currentPosition,
                        end = length,
                    )
                    addStringAnnotation(
                        tag = AnnotatedStringBuilder.EMOJI_PATH_TAG,
                        annotation = EmojiMap.getFullUrl(fileName),
                        start = currentPosition,
                        end = length,
                    )
                } else {
                    append(tag)
                }
                lastIndex = match.range.last + 1
            }
            append(text.substring(lastIndex))
        }
    }
}
