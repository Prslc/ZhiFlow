package com.prslc.zhiflow.parser.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import com.prslc.zhiflow.data.model.Formula
import com.prslc.zhiflow.data.model.ZhihuImage

@Immutable
data class InlineFormulaMeta(
    val formula: Formula,
    val inlineId: String
)

@Stable
data class ProcessedText(
    val content: AnnotatedString,
    val inlineMetas: List<InlineFormulaMeta> = emptyList()
)

@Stable
sealed class DetailElement {
    /** Plain text segment extracted from question HTML. */
    @Stable
    data class Text(val content: AnnotatedString) : DetailElement()
    /** Image extracted from a `<figure>` tag. */
    @Stable
    data class Image(val image: ZhihuImage) : DetailElement()
}

/**
 * Renderable content element produced by the parsing pipeline.
 *
 * Represents one atomic piece of rich content: text, image, formula, code block,
 * table, card, list item, blockquote, reference, or divider.
 */
@Stable
sealed interface RichTextElement {
    /** Section heading with level (h1-h6). */
    @Stable
    data class Heading(val content: AnnotatedString, val level: Int = 2) : RichTextElement
    /** Inline or block image. */
    @Stable
    data class Image(val data: ZhihuImage) : RichTextElement
    /** Standalone block-level LaTeX formula. */
    @Immutable
    data class FormulaBlock(val data: Formula) : RichTextElement
    /** Code block with optional language identifier. */
    @Immutable
    data class Code(val code: String, val lang: String?) : RichTextElement
    /** Collection of reference items (footnotes). */
    @Stable
    data class Reference(val items: List<AnnotatedString>) : RichTextElement
    /** Horizontal rule / divider. */
    @Immutable
    data object Divider : RichTextElement

    /** Quoted text block. */
    @Stable
    data class Blockquote(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>
    ) : RichTextElement

    /** List item with nesting support for ordered/unordered lists. */
    @Stable
    data class BulletItem(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>,
        val level: Int,
        val isOrdered: Boolean,
        val index: Int = 0
    ) : RichTextElement

    /** A single cell within a table. */
    @Stable
    data class TableCell(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>
    )

    /** Table with header row support. */
    @Stable
    data class Table(
        val rows: Int,
        val cols: Int,
        val cells: List<TableCell>,
        val hasHeader: Boolean
    ) : RichTextElement

    /** Link card with cover image, description, and content type metadata. */
    @Immutable
    data class Card(
        val cardType: String,
        val title: String,
        val url: String,
        val cover: String?,
        val desc: String?,
        val contentType: String?
    ) : RichTextElement

    /** Standard text paragraph with optional inline formulas. */
    @Stable
    data class ParsedText(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>
    ) : RichTextElement
}