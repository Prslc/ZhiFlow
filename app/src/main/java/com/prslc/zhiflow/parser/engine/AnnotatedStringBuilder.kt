package com.prslc.zhiflow.parser.engine

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.prslc.zhiflow.data.model.Mark
import com.prslc.zhiflow.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.parser.model.ProcessedText
import com.prslc.zhiflow.ui.theme.TextStyles

object AnnotatedStringBuilder {
    fun build(
        rawText: String,
        marks: List<Mark>,
        onFormulaFound: (formulaMark: Mark, position: Int) -> InlineFormulaMeta?,
        isDark: Boolean
    ): ProcessedText {
        val inlineMetas = mutableListOf<InlineFormulaMeta>()

        val annotated = buildAnnotatedString {
            val boundaries = (marks.flatMap { listOf(it.start, it.end) } + listOf(0, rawText.length))
                .distinct().sorted()

            for (i in 0 until boundaries.size - 1) {
                val start = boundaries[i]
                val end = boundaries[i + 1]
                if (start >= end) continue

                val activeMarks = marks.filter { it.start <= start && it.end >= end }
                val formulaMark = activeMarks.find { it.type == "formula" }

                if (formulaMark != null) {
                    onFormulaFound(formulaMark, length)?.let { meta ->
                        inlineMetas.add(meta)
                        appendInlineContent(meta.inlineId, "\uFFFD")
                        addStringAnnotation("INLINE_ID", meta.inlineId, length - 1, length)
                    }
                } else {
                    val spanStart = length
                    append(rawText.substring(start, end))
                    activeMarks.forEach { applyMarkStyle(it, spanStart, length, isDark) }
                }
            }
        }
        return ProcessedText(annotated, inlineMetas)
    }

    private fun AnnotatedString.Builder.applyMarkStyle(mark: Mark, start: Int, end: Int, isDark: Boolean) {
        when (mark.type) {
            "bold" -> addStyle(TextStyles.boldStyle, start, end)
            "italic" -> addStyle(TextStyles.italicStyle, start, end)
            "strikethrough" -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
            "code" -> addStyle(TextStyles.codeStyle(isDark), start, end)
            "link" -> {
                val url = mark.link?.href ?: mark.entityWord?.url
                if (!url.isNullOrEmpty()) {
                    addStringAnnotation("URL", url, start, end)
                    addStyle(TextStyles.linkStyle(isDark), start, end)
                }
            }
        }
    }
}