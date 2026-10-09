package com.prslc.zhiflow.data.remote.parser.engine

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.prslc.zhiflow.data.model.content.Mark
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.remote.parser.emoji.EmojiMap
import com.prslc.zhiflow.data.remote.parser.emoji.EmojiMatch
import com.prslc.zhiflow.data.remote.parser.emoji.EmojiParser
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.data.remote.parser.model.ProcessedText
import com.prslc.zhiflow.ui.theme.TextStyles

@Immutable
object AnnotatedStringBuilder {
    /**
     * Stands in for an inline formula in the built text; the inline content draws the image over
     * it.
     */
    const val FORMULA_PLACEHOLDER = "\uFFFD"

    /** Range of a `seg_like` mark, carrying the [SegmentLikeTarget.key] that identifies it. */
    const val SEGMENT_LIKE_TAG = "SEGMENT_LIKE"

    /** The speech-bubble placeholder at the end of that range, carrying the same key. */
    const val SEGMENT_LIKE_ICON_TAG = "SEGMENT_LIKE_ICON"

    /** Range that carries a url to open. Coloured by the theme, like [CODE_TAG]. */
    const val URL_TAG = "URL"

    /**
     * Range of a `code` mark. Its colours are the theme's, so the range is marked and not styled.
     */
    const val CODE_TAG = "CODE"

    /** Range of a `reference` mark, marked for the same reason as [CODE_TAG]. */
    const val REFERENCE_TAG = "REFERENCE"

    /** Range of an emoji, which the renderer draws from the asset its path names. */
    const val EMOJI_ID_TAG = "EMOJI_ID"

    /** Path of the asset one emoji is drawn from. */
    const val EMOJI_PATH_TAG = "EMOJI_PATH"

    /** Built-offset key of the inline content for that bubble. */
    fun segmentLikeIconId(position: Int) = "seg_like_icon_$position"

    /** Built-offset key of the inline content for that emoji. */
    fun emojiInlineId(position: Int) = "emoji_$position"

    /**
     * Build an [AnnotatedString] from raw text and a list of [Mark] style definitions.
     *
     * Segments text by mark boundaries, applies the span styles that carry no theme (bold, italic,
     * strikethrough), and invokes [onFormulaFound] for inline formula placeholders. A bracket that
     * names an emoji this app carries is replaced the same way -- one placeholder, two annotations
     * -- so the words around it keep the offsets the marks were measured against.
     *
     * Three kinds of range are annotated rather than styled, because what they look like is not
     * known here. A `seg_like` range's underline is solid or dashed with live like state; a link, a
     * `code` block and a `reference` take their colours from the theme, which flips while a body
     * stays parsed. Carrying a mode in the string would make the parse depend on it, and every
     * cache of a parsed body would have to be purged whenever the reader changed theme. The
     * renderer knows the mode and applies those three styles, reading the ranges back off these
     * annotations -- the same way a rebuilt string keeps carrying a `seg_like` range.
     *
     * A `seg_like` range ends where its bubble begins — the offset mapping that the tail of the
     * loop leaves behind points past the bubble, and the underline has to stop short of it. A range
     * with nothing said about it carries no bubble and ends at its own last character instead; its
     * underline is still there, and still opens the panel.
     *
     * @param onFormulaFound Returns null to drop the mark's raw text instead of placing inline
     *   content over it.
     * @param segmentLikes The ranges to mark up, in raw-text order. One whose
     *   [SegmentLikeTarget.commentCount] is zero is underlined but gets no bubble, and a range is
     *   marked to its [SegmentLikeTarget.passageEnd] rather than to its raw end.
     */
    fun build(
        rawText: String,
        marks: List<Mark>,
        onFormulaFound: (formulaMark: Mark, position: Int) -> InlineFormulaMeta?,
        segmentLikes: List<SegmentLikeTarget> = emptyList(),
    ): ProcessedText {
        val inlineMetas = mutableListOf<InlineFormulaMeta>()

        val (formulaMarks, styleMarks) = marks.partition { it.type == "formula" }

        val insertions = buildList {
            formulaMarks.forEach { add(Insertion.Replace(it.start, it.end, it)) }
            EmojiParser.knownEmoji(rawText).forEach { add(Insertion.Emoji(it)) }
            segmentLikes.filter { it.commentCount > 0 }
                .forEach { add(Insertion.Point(it.passageEnd, it)) }
        }.sortedBy { it.start }

        val rawToBuiltMap = IntArray(rawText.length + 1)
        // Keyed by the range, not by the offset a bubble sits at: two ranges can share an offset,
        // and an underline read off the wrong one would stop at a bubble that is not its own.
        val bubbleStarts = mutableMapOf<String, Int>()

        val annotated = buildAnnotatedString {
            var currentRawIndex = 0

            for (insertion in insertions) {
                val start = insertion.start.coerceIn(0, rawText.length)
                if (start < currentRawIndex) continue

                while (currentRawIndex < start) {
                    rawToBuiltMap[currentRawIndex] = length
                    append(rawText[currentRawIndex])
                    currentRawIndex++
                }

                when (insertion) {
                    is Insertion.Replace -> {
                        val end = insertion.end.coerceIn(start, rawText.length)
                        val insertionStart = length

                        onFormulaFound(insertion.mark, insertionStart)?.let { meta ->
                            inlineMetas.add(meta)
                            appendInlineContent(meta.inlineId, FORMULA_PLACEHOLDER)
                        }

                        while (currentRawIndex < end) {
                            rawToBuiltMap[currentRawIndex] = insertionStart
                            currentRawIndex++
                        }
                    }

                    is Insertion.Emoji -> {
                        val end = insertion.match.end.coerceIn(start, rawText.length)
                        val insertionStart = length
                        val inlineId = emojiInlineId(insertionStart)

                        appendInlineContent(inlineId, insertion.match.tag)
                        addStringAnnotation(EMOJI_ID_TAG, inlineId, insertionStart, length)
                        addStringAnnotation(
                            EMOJI_PATH_TAG,
                            EmojiMap.getFullUrl(insertion.match.fileName),
                            insertionStart,
                            length,
                        )

                        while (currentRawIndex < end) {
                            rawToBuiltMap[currentRawIndex] = insertionStart
                            currentRawIndex++
                        }
                    }

                    is Insertion.Point -> {
                        val insertionStart = length
                        appendInlineContent(
                            segmentLikeIconId(insertionStart),
                            FORMULA_PLACEHOLDER,
                        )
                        addStringAnnotation(
                            SEGMENT_LIKE_ICON_TAG,
                            insertion.target.key,
                            insertionStart,
                            length,
                        )
                        bubbleStarts[insertion.target.key] = insertionStart
                        // Zero-width in raw text: the bubble sits between [start] and the character
                        // after it, so every offset mapping stays as it was.
                    }
                }
            }

            while (currentRawIndex <= rawText.length) {
                rawToBuiltMap[currentRawIndex] = length
                if (currentRawIndex < rawText.length) {
                    append(rawText[currentRawIndex])
                }
                currentRawIndex++
            }

            styleMarks.forEach { mark ->
                val markStart = mark.start.coerceIn(0, rawText.length)
                val markEnd = mark.end.coerceIn(0, rawText.length)

                val finalStart = rawToBuiltMap[markStart]
                val finalEnd = rawToBuiltMap[markEnd]

                if (finalStart < finalEnd) {
                    applyMarkStyle(mark, finalStart, finalEnd)
                }
            }

            segmentLikes.forEach { target ->
                val end = target.passageEnd.coerceIn(0, rawText.length)
                val finalStart = rawToBuiltMap[target.rawStart.coerceIn(0, rawText.length)]
                val finalEnd = bubbleStarts[target.key] ?: rawToBuiltMap[end]

                if (finalStart < finalEnd) {
                    addStringAnnotation(SEGMENT_LIKE_TAG, target.key, finalStart, finalEnd)
                }
            }
        }

        return ProcessedText(annotated, inlineMetas, segmentLikes)
    }

    private sealed interface Insertion {
        val start: Int

        /** Replaces `[start, end)` of the raw text with one inline-content placeholder. */
        data class Replace(override val start: Int, val end: Int, val mark: Mark) : Insertion

        /** Replaces an emoji's bracket with the asset that draws it. */
        data class Emoji(val match: EmojiMatch) : Insertion {
            override val start: Int get() = match.start
        }

        /** Inserts one inline-content placeholder between two raw characters. */
        data class Point(override val start: Int, val target: SegmentLikeTarget) : Insertion
    }
}

/**
 * Applies what a mark says without asking the theme anything: the styles that carry no colour are
 * set here, and the marks whose colour belongs to the theme are left as annotations for the
 * renderer to find.
 */
private fun AnnotatedString.Builder.applyMarkStyle(
    mark: Mark,
    start: Int,
    end: Int,
) {
    when (mark.type) {
        "bold" -> addStyle(TextStyles.boldStyle, start, end)
        "italic" -> addStyle(TextStyles.italicStyle, start, end)
        "strikethrough" -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
        "code" -> addStringAnnotation(AnnotatedStringBuilder.CODE_TAG, "", start, end)
        "reference" -> addStringAnnotation(AnnotatedStringBuilder.REFERENCE_TAG, "", start, end)
        "link" -> {
            val url = mark.link?.href ?: mark.entityWord?.url
            if (!url.isNullOrEmpty()) {
                addStringAnnotation(AnnotatedStringBuilder.URL_TAG, url, start, end)
            }
        }
    }
}
