package com.prslc.zhiflow.parser

import com.prslc.zhiflow.core.utils.JsonHelper
import com.prslc.zhiflow.data.model.Card
import com.prslc.zhiflow.data.model.Mark
import com.prslc.zhiflow.data.model.Paragraph
import com.prslc.zhiflow.data.model.Segment
import com.prslc.zhiflow.parser.engine.AnnotatedStringBuilder
import com.prslc.zhiflow.parser.engine.FormulaHandler
import com.prslc.zhiflow.parser.engine.TableParser
import com.prslc.zhiflow.parser.model.ProcessedText
import com.prslc.zhiflow.parser.model.RichTextElement

object ContentParser {
    fun transform(
        segments: List<Segment>,
        isDark: Boolean = false
    ): List<RichTextElement> {
        val listCounter = OrderedListCounter()
        return segments.flatMap { segment ->
            when (segment.type) {
                "paragraph" -> processParagraph(segment.paragraph, isDark)

                "heading" -> segment.heading?.let {
                    val p = parseContent(it.text, it.marks, isDark)
                    listOf(RichTextElement.Heading(p.content, it.level))
                } ?: emptyList()

                "list_node" -> segment.listNode?.items?.map { item ->
                    val p = parseContent(item.text, item.marks, isDark)
                    RichTextElement.BulletItem(
                        p.content, p.inlineMetas, item.indentLevel,
                        segment.listNode.type == "ordered", listCounter.next(item.indentLevel)
                    )
                } ?: emptyList()

                "blockquote" -> segment.blockquote?.let {
                    val p = parseContent(it.text, it.marks, isDark)
                    listOf(RichTextElement.Blockquote(p.content, p.inlineMetas))
                } ?: emptyList()

                "table" -> segment.table?.let {
                    listOf(TableParser.parse(it) { text ->
                        parseContent(text, emptyList(), isDark)
                    })
                } ?: emptyList()

                "card" -> parseCard(segment.card)
                "image" -> segment.image?.let { listOf(RichTextElement.Image(it)) } ?: emptyList()
                "code_block" -> segment.codeBlock?.let {
                    listOf(RichTextElement.Code(it.content, it.language))
                } ?: emptyList()

                "hr" -> listOf(RichTextElement.Divider)
                else -> emptyList()
            }
        }
    }

    private fun parseContent(
        rawText: String,
        marks: List<Mark>,
        isDark: Boolean
    ): ProcessedText {
        return AnnotatedStringBuilder.build(
            rawText = rawText,
            marks = marks,
            isDark = isDark,
            onFormulaFound = { mark, pos ->
                mark.formula?.let {
                    FormulaHandler.prepareInlineMeta(it, pos)
                }
            }
        )
    }

    private fun processParagraph(
        paragraph: Paragraph?,
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
            val processed = parseContent(rawText, marks, isDark)
            return listOf(RichTextElement.ParsedText(processed.content, processed.inlineMetas))
        }

        val elements = mutableListOf<RichTextElement>()
        var lastIndex = 0

        blockFormulaMarks.forEach { mark ->
            if (mark.start > lastIndex) {
                val subText = rawText.substring(lastIndex, mark.start)
                if (subText.isNotBlank() && subText != "\n") {
                    val subMarks = marks.filter { it.start >= lastIndex && it.end <= mark.start }
                        .map { it.copy(start = it.start - lastIndex, end = it.end - lastIndex) }
                    val processed = parseContent(subText, subMarks, isDark)
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
            if (subText.isNotBlank() && subText != "\n") {
                val subMarks = marks.filter { it.start >= lastIndex }
                    .map { it.copy(start = it.start - lastIndex, end = it.end - lastIndex) }
                val processed = parseContent(subText, subMarks, isDark)
                elements.add(RichTextElement.ParsedText(processed.content, processed.inlineMetas))
            }
        }

        return elements
    }

    private fun parseCard(card: Card?) = card?.let {
        val extra = JsonHelper.parseExtraInfo(it.extraInfo)
        listOf(
            RichTextElement.Card(
                cardType = it.cardType,
                title = it.title ?: extra?.title ?: "No title",
                url = it.url ?: extra?.url ?: "",
                cover = extra?.cover?.takeIf { c -> c.isNotBlank() } ?: it.cover,
                desc = JsonHelper.cleanHtmlDesc(extra?.desc),
                contentType = it.contentType ?: extra?.contentType
            ))
    } ?: emptyList()

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