package com.prslc.zhiflow.parser

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.model.LatexConfig
import com.prslc.zhiflow.data.model.Formula
import com.prslc.zhiflow.data.model.Mark
import com.prslc.zhiflow.data.model.Paragraph
import com.prslc.zhiflow.data.model.Segment
import com.prslc.zhiflow.data.model.ZhihuImage
import com.prslc.zhiflow.ui.theme.TextStyles
import com.prslc.zhiflow.utils.JsonHelper
import com.prslc.zhiflow.utils.cleanLatex

data class InlineFormulaMeta(
    val formula: Formula,
    val inlineId: String,
    val width: TextUnit,
    val height: TextUnit
)

data class CommentContent(
    val text: AnnotatedString,
    val images: List<ZhihuImage>
)

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

data class ProcessedText(
    val content: AnnotatedString,
    val inlineMetas: List<InlineFormulaMeta>
)

object ContentParser {
    fun transform(
        segments: List<Segment>,
        measurer: LatexMeasurerState,
        density: Density,
        config: LatexConfig,
        isDark: Boolean = false
    ): List<RichTextElement> {
        return segments.flatMap { segment ->
            when (segment.type) {
                "paragraph" -> processParagraph(
                    segment.paragraph,
                    measurer,
                    density,
                    config,
                    isDark
                )

                "heading" -> segment.heading?.let {
                    val processed =
                        parseContent(it.text, it.marks, measurer, density, config, isDark)
                    listOf(RichTextElement.Heading(processed.content, it.level))
                } ?: emptyList()

                "list_node" -> {
                    val counter = OrderedListCounter()
                    segment.listNode?.items?.map { item ->
                        val processed =
                            parseContent(item.text, item.marks, measurer, density, config, isDark)
                        RichTextElement.BulletItem(
                            content = processed.content,
                            inlineMetas = processed.inlineMetas,
                            level = item.indentLevel,
                            isOrdered = segment.listNode.type == "ordered",
                            index = counter.next(item.indentLevel)
                        )
                    } ?: emptyList()
                }

                "blockquote" -> segment.blockquote?.let {
                    val processed =
                        parseContent(it.text, it.marks, measurer, density, config, isDark)
                    listOf(
                        RichTextElement.Blockquote(
                            content = processed.content,
                            inlineMetas = processed.inlineMetas
                        )
                    )
                } ?: emptyList()

                "image" -> segment.image?.let { listOf(RichTextElement.Image(it)) } ?: emptyList()

                "code_block" -> segment.codeBlock?.let {
                    listOf(
                        RichTextElement.Code(
                            it.content,
                            it.language
                        )
                    )
                } ?: emptyList()

                "reference_block" -> segment.referenceBlock?.let { block ->
                    val items = block.items.map {
                        parseContent(
                            it.text,
                            it.marks,
                            measurer,
                            density,
                            config,
                            isDark
                        ).content
                    }
                    listOf(RichTextElement.Reference(items))
                } ?: emptyList()

                "table" -> segment.table?.let { table ->
                    listOf(
                        RichTextElement.Table(
                            rows = table.rowCount,
                            cols = table.columnCount,
                            cells = table.cells.map { cellRawText ->
                                val processed = parseContent(
                                    cellRawText,
                                    emptyList(),
                                    measurer,
                                    density,
                                    config,
                                    isDark
                                )
                                RichTextElement.TableCell(processed.content, processed.inlineMetas)
                            },
                            hasHeader = table.hasHeadRow
                        )
                    )
                } ?: emptyList()

                "card" -> segment.card?.let { card ->
                    val extra = JsonHelper.parseExtraInfo(card.extraInfo)
                    val finalCover = extra?.cover?.takeIf { it.isNotBlank() } ?: card.cover

                    listOf(
                        RichTextElement.Card(
                            cardType = card.cardType,
                            title = card.title ?: extra?.title ?: "No title",
                            url = card.url ?: extra?.url ?: "",
                            cover = finalCover,
                            desc = JsonHelper.cleanHtmlDesc(extra?.desc),
                            contentType = card.contentType ?: extra?.contentType
                        )
                    )
                } ?: emptyList()

                "hr" -> listOf(RichTextElement.Divider)
                else -> emptyList()
            }
        }
    }

    private fun processParagraph(
        paragraph: Paragraph?,
        measurer: LatexMeasurerState,
        density: Density,
        config: LatexConfig,
        isDark: Boolean
    ): List<RichTextElement> {
        if (paragraph == null) return emptyList()

        val rawText = paragraph.text
        val marks = paragraph.marks

        val isStrictBlock = rawText.trim() == "[公式]" && marks.any { it.type == "formula" }
        val blockFormulaMarks = marks.filter { mark ->
            mark.type == "formula" && mark.formula?.let {
                it.content.contains("\\\\") || it.content.contains("\\begin{") || isStrictBlock
            } == true
        }.sortedBy { it.start }

        if (blockFormulaMarks.isEmpty()) {
            val processed = parseContent(rawText, marks, measurer, density, config, isDark)
            return listOf(RichTextElement.ParsedText(processed.content, processed.inlineMetas))
        }

        val elements = mutableListOf<RichTextElement>()
        var lastIndex = 0

        blockFormulaMarks.forEach { mark ->
            if (mark.start > lastIndex) {
                val subText = rawText.substring(lastIndex, mark.start)
                val subMarks = marks.filter { it.start >= lastIndex && it.end <= mark.start }
                    .map { it.copy(start = it.start - lastIndex, end = it.end - lastIndex) }

                if (subText.isNotBlank() && subText != "\n") {
                    val processed =
                        parseContent(subText, subMarks, measurer, density, config, isDark)
                    elements.add(
                        RichTextElement.ParsedText(
                            processed.content,
                            processed.inlineMetas
                        )
                    )
                }
            }
            mark.formula?.let { elements.add(RichTextElement.FormulaBlock(it)) }
            lastIndex = mark.end
        }

        if (lastIndex < rawText.length) {
            val subText = rawText.substring(lastIndex)
            val subMarks = marks.filter { it.start >= lastIndex }
                .map { it.copy(start = it.start - lastIndex, end = it.end - lastIndex) }

            if (subText.isNotBlank() && subText != "\n") {
                val processed = parseContent(subText, subMarks, measurer, density, config, isDark)
                elements.add(RichTextElement.ParsedText(processed.content, processed.inlineMetas))
            }
        }

        return elements
    }

    private fun parseContent(
        rawText: String,
        marks: List<Mark>,
        measurer: LatexMeasurerState,
        density: Density,
        config: LatexConfig,
        isDark: Boolean
    ): ProcessedText {
        val inlineMetas = mutableListOf<InlineFormulaMeta>()

        val annotated = buildAnnotatedString {
            val formulaMarks =
                marks.filter { it.type == "formula" && it.formula != null }.sortedBy { it.start }
            var currentRawIndex = 0
            val offsets = mutableListOf<Pair<Int, Int>>()

            formulaMarks.forEach { mark ->
                append(rawText.substring(currentRawIndex, mark.start))

                val formulaData = mark.formula!!
                val placeholderPos = length
                val inlineId = "f_${placeholderPos}_${formulaData.content.hashCode()}"

                // measurement
                val dims = measurer.measure(formulaData.content.cleanLatex(), config, isDark)
                val widthSp = with(density) { dims?.widthPx?.toSp() ?: 20.sp }
                val heightSp = with(density) { dims?.heightPx?.toSp() ?: 20.sp }

                if (dims != null) {
                    inlineMetas.add(
                        InlineFormulaMeta(
                            formula = formulaData,
                            inlineId = inlineId,
                            width = widthSp,
                            height = heightSp
                        )
                    )
                    appendInlineContent(inlineId, "\uFFFD")
                    addStringAnnotation("INLINE_ID", inlineId, placeholderPos, placeholderPos + 1)
                }

                offsets.add(mark.start to (mark.end - mark.start - 1))
                currentRawIndex = mark.end
            }
            append(rawText.substring(currentRawIndex))

            marks.filter { it.type != "formula" }.forEach { mark ->
                val newStart = calculateNewIndex(mark.start, offsets)
                val newEnd = calculateNewIndex(mark.end, offsets)
                val safeStart = newStart.coerceIn(0, length)
                val safeEnd = newEnd.coerceIn(0, length)
                if (safeStart >= safeEnd) return@forEach

                when (mark.type) {
                    "bold" -> addStyle(TextStyles.boldStyle, safeStart, safeEnd)

                    "italic" -> addStyle(TextStyles.italicStyle, safeStart, safeEnd)

                    "strikethrough" -> addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                        safeStart,
                        safeEnd
                    )

                    "code" -> addStyle(
                        TextStyles.codeStyle(isDark),
                        safeStart,
                        safeEnd
                    )

                    "reference" -> {
                        addStyle(
                            TextStyles.referenceStyle(isDark),
                            safeStart,
                            safeEnd
                        )
                        mark.reference?.title?.let {
                            addStringAnnotation("REF_TITLE", it, safeStart, safeEnd)
                        }
                    }

                    "link" -> {
                        val url = mark.link?.href ?: mark.entityWord?.url
                        if (!url.isNullOrEmpty()) {
                            addStringAnnotation("URL", url, safeStart, safeEnd)
                            addStyle(
                                TextStyles.linkStyle(isDark),
                                safeStart,
                                safeEnd
                            )
                        }
                    }
                }
            }
        }
        return ProcessedText(annotated, inlineMetas)
    }

    private fun calculateNewIndex(oldIndex: Int, offsets: List<Pair<Int, Int>>): Int {
        var totalShift = 0
        for ((origStart, reduced) in offsets) {
            if (oldIndex > origStart) totalShift += reduced else break
        }
        return (oldIndex - totalShift).coerceAtLeast(0)
    }

    private class OrderedListCounter {
        private val counts = mutableMapOf<Int, Int>()
        fun next(level: Int): Int {
            val nextIdx = (counts[level] ?: 0) + 1
            counts[level] = nextIdx
            counts.keys.removeAll { it > level }
            return nextIdx
        }
    }
}