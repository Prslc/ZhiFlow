package com.prslc.zhiflow.data.remote.parser.engine

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.prslc.zhiflow.data.model.content.Mark
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.data.remote.parser.model.ProcessedText
import com.prslc.zhiflow.ui.theme.TextStyles

@Immutable
object AnnotatedStringBuilder {
    /** Stands in for an inline formula in the built text; the inline content draws the image over it. */
    const val FORMULA_PLACEHOLDER = "\uFFFD"

    /** Range of a `seg_like` mark, carrying the [SegmentLikeTarget.key] that identifies it. */
    const val SEGMENT_LIKE_TAG = "SEGMENT_LIKE"

    /** The speech-bubble placeholder at the end of that range, carrying the same key. */
    const val SEGMENT_LIKE_ICON_TAG = "SEGMENT_LIKE_ICON"

    private const val NO_BUBBLE = -1

    /** Built-offset key of the inline content for that bubble. */
    fun segmentLikeIconId(position: Int) = "seg_like_icon_$position"

    /**
     * Build an [AnnotatedString] from raw text and a list of [Mark] style definitions.
     *
     * Segments text by mark boundaries, applies span styles (bold, italic, code, link, etc.),
     * and invokes [onFormulaFound] for inline formula placeholders.
     *
     * `seg_like` ranges are annotated rather than styled: whether their underline is solid or
     * dashed depends on live like state, which the built string cannot hold. Each range ends where
     * its bubble begins — the offset mapping that the tail of the loop leaves behind points past
     * the bubble, and the underline has to stop short of it. A range with nothing said about it
     * carries no bubble and ends at its own last character instead; its underline is still there,
     * and still opens the panel.
     *
     * @param onFormulaFound returns null to drop the mark's raw text instead of placing inline
     *   content over it.
     * @param segmentLikes the ranges to mark up, in raw-text order. One whose
     *   [SegmentLikeTarget.commentCount] is zero is underlined but gets no bubble.
     */
    fun build(
        rawText: String,
        marks: List<Mark>,
        onFormulaFound: (formulaMark: Mark, position: Int) -> InlineFormulaMeta?,
        isDark: Boolean,
        segmentLikes: List<SegmentLikeTarget> = emptyList(),
    ): ProcessedText {
        val inlineMetas = mutableListOf<InlineFormulaMeta>()

        val (formulaMarks, styleMarks) = marks.partition { it.type == "formula" }

        val insertions = buildList {
            formulaMarks.forEach { add(Insertion.Replace(it.start, it.end, it)) }
            segmentLikes.filter { it.commentCount > 0 }
                .forEach { add(Insertion.Point(it.rawEnd, it)) }
        }.sortedBy { it.start }

        val rawToBuiltMap = IntArray(rawText.length + 1)
        val bubbleStarts = IntArray(rawText.length + 1) { NO_BUBBLE }

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
                            addStringAnnotation("INLINE_ID", meta.inlineId, insertionStart, length)
                        }

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
                        bubbleStarts[start] = insertionStart
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
                    applyMarkStyle(mark, finalStart, finalEnd, isDark)
                }
            }

            segmentLikes.forEach { target ->
                val rawEnd = target.rawEnd.coerceIn(0, rawText.length)
                val finalStart = rawToBuiltMap[target.rawStart.coerceIn(0, rawText.length)]
                val bubbleStart = bubbleStarts[rawEnd]
                val finalEnd = if (bubbleStart == NO_BUBBLE) rawToBuiltMap[rawEnd] else bubbleStart

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

        /** Inserts one inline-content placeholder between two raw characters. */
        data class Point(override val start: Int, val target: SegmentLikeTarget) : Insertion
    }
}

private fun AnnotatedString.Builder.applyMarkStyle(
    mark: Mark,
    start: Int,
    end: Int,
    isDark: Boolean
) {
    when (mark.type) {
        "bold" -> addStyle(TextStyles.boldStyle, start, end)
        "italic" -> addStyle(TextStyles.italicStyle, start, end)
        "strikethrough" -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
        "code" -> addStyle(TextStyles.codeStyle(isDark), start, end)
        "reference" -> addStyle(TextStyles.referenceStyle(isDark), start, end)
        "link" -> {
            val url = mark.link?.href ?: mark.entityWord?.url
            if (!url.isNullOrEmpty()) {
                addStringAnnotation("URL", url, start, end)
                addStyle(TextStyles.linkStyle(isDark), start, end)
            }
        }
    }
}
