package com.prslc.zhiflow.parser.model

import androidx.compose.ui.text.AnnotatedString
import com.prslc.zhiflow.data.model.Formula
import com.prslc.zhiflow.data.model.ZhihuImage

data class InlineFormulaMeta(
    val formula: Formula,
    val inlineId: String
)

data class ProcessedText(
    val content: AnnotatedString,
    val inlineMetas: List<InlineFormulaMeta> = emptyList()
)

sealed class DetailElement {
    data class Text(val content: AnnotatedString) : DetailElement()
    data class Image(val image: ZhihuImage) : DetailElement()
}

sealed interface RichTextElement {
    data class Heading(val content: AnnotatedString, val level: Int = 2) : RichTextElement
    data class Image(val data: ZhihuImage) : RichTextElement
    data class FormulaBlock(val data: Formula) : RichTextElement
    data class Code(val code: String, val lang: String?) : RichTextElement
    data class Reference(val items: List<AnnotatedString>) : RichTextElement
    data object Divider : RichTextElement

    data class Blockquote(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>
    ) : RichTextElement

    data class BulletItem(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>,
        val level: Int,
        val isOrdered: Boolean,
        val index: Int = 0
    ) : RichTextElement

    data class TableCell(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>
    )

    data class Table(
        val rows: Int,
        val cols: Int,
        val cells: List<TableCell>,
        val hasHeader: Boolean
    ) : RichTextElement

    data class Card(
        val cardType: String,
        val title: String,
        val url: String,
        val cover: String?,
        val desc: String?,
        val contentType: String?
    ) : RichTextElement

    data class ParsedText(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>
    ) : RichTextElement
}