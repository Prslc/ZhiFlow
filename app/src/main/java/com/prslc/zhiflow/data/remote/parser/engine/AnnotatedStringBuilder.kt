package com.prslc.zhiflow.data.remote.parser.engine

import androidx.compose.runtime.Immutable
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.prslc.zhiflow.data.model.content.Mark
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.data.remote.parser.model.ProcessedText
import com.prslc.zhiflow.ui.theme.TextStyles

@Immutable
object AnnotatedStringBuilder {
    /**
     * Build an [AnnotatedString] from raw text and a list of [Mark] style definitions.
     *
     * Segments text by mark boundaries, applies span styles (bold, italic, code, link, etc.),
     * and invokes [onFormulaFound] for inline formula placeholders.
     */
    fun build(
        rawText: String,
        marks: List<Mark>,
        onFormulaFound: (formulaMark: Mark, position: Int) -> InlineFormulaMeta?,
        isDark: Boolean
    ): ProcessedText {
        val inlineMetas = mutableListOf<InlineFormulaMeta>()

        val (formulaMarks, styleMarks) = marks.partition { it.type == "formula" }
        val sortedFormulae = formulaMarks.sortedBy { it.start }

        val rawToBuiltMap = IntArray(rawText.length + 1)

        val annotated = buildAnnotatedString {
            var currentRawIndex = 0

            for (formula in sortedFormulae) {
                val formulaStart = formula.start.coerceIn(0, rawText.length)
                val formulaEnd = formula.end.coerceIn(0, rawText.length)
                if (formulaStart < currentRawIndex) continue

                while (currentRawIndex < formulaStart) {
                    rawToBuiltMap[currentRawIndex] = length
                    append(rawText[currentRawIndex])
                    currentRawIndex++
                }

                val formulaStartInBuilt = length
                onFormulaFound(formula, formulaStartInBuilt)?.let { meta ->
                    inlineMetas.add(meta)
                    appendInlineContent(meta.inlineId, "\uFFFD")
                    addStringAnnotation("INLINE_ID", meta.inlineId, formulaStartInBuilt, length)
                }

                while (currentRawIndex < formulaEnd) {
                    rawToBuiltMap[currentRawIndex] = formulaStartInBuilt
                    currentRawIndex++
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
        }

        return ProcessedText(annotated, inlineMetas)
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
