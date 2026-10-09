package com.prslc.zhiflow.data.remote.parser.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import com.prslc.zhiflow.data.model.content.Formula
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.model.content.ZhihuImage

@Immutable
data class InlineFormulaMeta(
    val formula: Formula,
    val inlineId: String,
)

@Stable
data class ProcessedText(
    val content: AnnotatedString,
    val inlineMetas: List<InlineFormulaMeta> = emptyList(),
    val segmentLikes: List<SegmentLikeTarget> = emptyList(),
)

/**
 * One run of code that shares a colour, as a half-open `[start, end)` range.
 *
 * Runs never overlap and never cover text the tokenizer said nothing about, so a renderer paints
 * them in the order it is handed them and the gaps keep the block's own colour.
 */
@Immutable
data class CodeToken(
    val start: Int,
    val end: Int,
    val kind: CodeTokenKind,
)

/** What a [CodeToken] is, one entry per set the tokenizer answers with. */
enum class CodeTokenKind {
    Mark,
    Punctuation,
    Keyword,
    String,
    Literal,
    Annotation,
    Comment,
    MultilineComment,
}

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
    data class Heading(val content: AnnotatedString, val inlineMetas: List<InlineFormulaMeta> = emptyList(), val level: Int = 2) : RichTextElement
    /** Inline or block image. */
    @Stable
    data class Image(val data: ZhihuImage) : RichTextElement
    /** Standalone block-level LaTeX formula. */
    @Immutable
    data class FormulaBlock(val data: Formula) : RichTextElement
    /**
     * Code block with optional language identifier.
     *
     * [tokens] carries no colour of its own — which colour a kind gets is the renderer's, and the
     * block is cached across both themes on that account.
     */
    @Immutable
    data class Code(
        val code: String,
        val lang: String?,
        val tokens: List<CodeToken>,
    ) : RichTextElement
    /** Collection of reference items (footnotes). */
    @Stable
    data class Reference(val items: List<ParsedText>) : RichTextElement
    /** Horizontal rule / divider. */
    @Immutable
    data object Divider : RichTextElement

    /** Quoted text block. */
    @Stable
    data class Blockquote(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>,
        override val segmentLikes: List<SegmentLikeTarget> = emptyList(),
    ) : RichTextElement, SegLikeHost

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
        val inlineMetas: List<InlineFormulaMeta>,
    )

    /** Table with header row support. */
    @Stable
    data class Table(
        val rows: Int,
        val cols: Int,
        val cells: List<TableCell>,
        val hasHeader: Boolean
    ) : RichTextElement

    /** Link card with cover image, content type metadata, and the counts it reports. */
    @Immutable
    data class Card(
        val cardType: String,
        val title: String,
        val url: String,
        val cover: String?,
        val contentType: String?,
        val voteCount: Int = 0,
        val commentCount: Int = 0,
    ) : RichTextElement

    /** Standard text paragraph with optional inline formulas. */
    @Stable
    data class ParsedText(
        val content: AnnotatedString,
        val inlineMetas: List<InlineFormulaMeta>,
        override val segmentLikes: List<SegmentLikeTarget> = emptyList(),
    ) : RichTextElement, SegLikeHost
}

/**
 * An element whose text can carry `seg_like` ranges, and so can open a passage panel.
 *
 * A paragraph and a quote are the two the API was seen to put those marks on. Every other host is
 * parsed without them: a range marked up in one would draw a bubble nothing answers.
 */
interface SegLikeHost {
    val segmentLikes: List<SegmentLikeTarget>
}
