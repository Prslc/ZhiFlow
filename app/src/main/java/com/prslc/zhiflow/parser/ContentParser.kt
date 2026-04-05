package com.prslc.zhiflow.parser

import android.text.Html
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.data.model.Formula
import com.prslc.zhiflow.data.model.Mark
import com.prslc.zhiflow.data.model.Paragraph
import com.prslc.zhiflow.data.model.Segment
import com.prslc.zhiflow.data.model.ZhihuImage
import com.prslc.zhiflow.utils.JsonHelper
import kotlinx.serialization.json.Json

data class CommentContent(
    val text: AnnotatedString,
    val images: List<ZhihuImage>
)

sealed interface RichTextElement {
    data class Text(val content: AnnotatedString) : RichTextElement
    data class Heading(val content: AnnotatedString, val level: Int = 2) : RichTextElement
    data class Image(val data: ZhihuImage) : RichTextElement
    data class FormulaBlock(val data: Formula) : RichTextElement
    data class Blockquote(val content: AnnotatedString) : RichTextElement
    data class Code(val code: String, val lang: String?) : RichTextElement
    data class Reference(val items: List<AnnotatedString>) : RichTextElement
    data object Divider : RichTextElement

    data class BulletItem(
        val content: AnnotatedString,
        val level: Int,
        val isOrdered: Boolean,
        val index: Int = 0  // ordered list
    ) : RichTextElement

    data class Table(
        val rows: Int,
        val cols: Int,
        val cells: List<AnnotatedString>,
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
}

object ContentParser {

    fun transform(segments: List<Segment>): List<RichTextElement> {
        return segments.flatMap { segment ->
            when (segment.type) {
                "paragraph" -> {
                    processParagraph(segment.paragraph)
                }

                "heading" -> {
                    segment.heading?.let {
                        listOf(RichTextElement.Heading(parseContent(it.text, it.marks), it.level))
                    } ?: emptyList()
                }

                "list_node" -> {
                    val counter = OrderedListCounter()
                    segment.listNode?.items?.map { item ->
                        RichTextElement.BulletItem(
                            content = parseContent(item.text, item.marks),
                            level = item.indentLevel,
                            isOrdered = segment.listNode.type == "ordered",
                            index = counter.next(item.indentLevel)
                        )
                    } ?: emptyList()
                }

                "blockquote" -> {
                    segment.blockquote?.let {
                        listOf(RichTextElement.Blockquote(parseContent(it.text, it.marks)))
                    } ?: emptyList()
                }

                "image" -> {
                    segment.image?.let { listOf(RichTextElement.Image(it)) } ?: emptyList()
                }

                "code_block" -> {
                    segment.codeBlock?.let { listOf(RichTextElement.Code(it.content, it.language)) }
                        ?: emptyList()
                }

                "reference_block" -> {
                    segment.referenceBlock?.let { block ->
                        val items = block.items.map { parseContent(it.text, it.marks) }
                        listOf(RichTextElement.Reference(items))
                    } ?: emptyList()
                }

                "table" -> {
                    segment.table?.let {
                        listOf(
                            RichTextElement.Table(
                                rows = it.rowCount,
                                cols = it.columnCount,
                                cells = it.cells.map { cellText ->
                                    parseContent(cellText, emptyList())
                                },
                                hasHeader = it.hasHeadRow
                            )
                        )
                    } ?: emptyList()
                }

                "card" -> {
                    segment.card?.let { card ->
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
                }

                "hr" -> listOf(RichTextElement.Divider)
                else -> emptyList()
            }
        }
    }

    private fun processParagraph(paragraph: Paragraph?): List<RichTextElement> {
        if (paragraph == null) return emptyList()

        val formulaMark = paragraph.marks.find { it.type == "formula" }
        if (paragraph.text.trim() == "[公式]" && formulaMark != null) {
            val formulaData = formulaMark.formula
            return if (formulaData != null) {
                listOf(RichTextElement.FormulaBlock(formulaData))
            } else {
                emptyList()
            }
        }

        return listOf(RichTextElement.Text(parseContent(paragraph.text, paragraph.marks)))
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

    private fun parseContent(rawText: String, marks: List<Mark>): AnnotatedString {
        return buildAnnotatedString {
            val formulaMarks = marks.filter { it.type == "formula" && it.formula != null }
                .sortedBy { it.start }

            var currentRawIndex = 0
            val offsets = mutableListOf<Pair<Int, Int>>()

            formulaMarks.forEach { mark ->
                append(rawText.substring(currentRawIndex, mark.start))

                val formulaData = mark.formula!!
                val placeholderPos = length
                val inlineId = "f_$placeholderPos"

                appendInlineContent(inlineId, "[f]")

                addStringAnnotation(
                    tag = "INLINE_FORMULA_DATA",
                    annotation = Json.encodeToString(formulaData),
                    start = placeholderPos,
                    end = placeholderPos + 1
                )

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
                    "bold" -> addStyle(
                        SpanStyle(fontWeight = FontWeight.ExtraBold),
                        safeStart,
                        safeEnd
                    )

                    "italic" -> addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        safeStart,
                        safeEnd
                    )

                    "strikethrough" -> addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                        safeStart,
                        safeEnd
                    )

                    "code" -> addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Gray.copy(alpha = 0.1f)
                        ), safeStart, safeEnd
                    )

                    "reference" -> {
                        addStyle(
                            style = SpanStyle(
                                fontSize = 12.sp,
                                baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E88E5)
                            ),
                            safeStart, safeEnd
                        )
                        mark.reference?.title?.let { title ->
                            addStringAnnotation("REF_TITLE", title, safeStart, safeEnd)
                        }
                    }

                    "link" -> {
                        val url = mark.link?.href ?: mark.entityWord?.url
                        if (!url.isNullOrEmpty()) {
                            addStringAnnotation("URL", url, safeStart, safeEnd)
                            addStyle(SpanStyle(color = Color(0xFF1E88E5)), safeStart, safeEnd)
                        }
                    }
                }
            }
        }
    }

    private fun calculateNewIndex(oldIndex: Int, offsets: List<Pair<Int, Int>>): Int {
        var totalShift = 0
        for ((origStart, reduced) in offsets) {
            if (oldIndex > origStart) {
                totalShift += reduced
            } else break
        }
        return (oldIndex - totalShift).coerceAtLeast(0)
    }

    fun parseCommentHtml(html: String): CommentContent {
        val extractedImages = mutableListOf<ZhihuImage>()

        val imgRegex = """<a[^>]+class="comment_img"[^>]*>.*?</a>""".toRegex()
        val hrefRegex = """href="([^"]+)"""".toRegex()
        val widthRegex = """data-width="(\d+)"""".toRegex()
        val heightRegex = """data-height="(\d+)"""".toRegex()

        var processedHtml = html
        imgRegex.findAll(html).forEach { match ->
            val fullTag = match.value
            val url = hrefRegex.find(fullTag)?.groupValues?.get(1) ?: ""

            if (url.isNotEmpty()) {
                val w = widthRegex.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val h = heightRegex.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val isGif = url.lowercase().contains(".gif")

                extractedImages.add(
                    ZhihuImage(
                        urls = listOf(url),
                        width = w,
                        height = h,
                        isGif = isGif,
                        description = "",
                    )
                )
            }
            processedHtml = processedHtml.replace(fullTag, "")
        }

        // Text
        val spanned = Html.fromHtml(processedHtml, Html.FROM_HTML_MODE_COMPACT)

        val annotatedText = buildAnnotatedString {
            val rawText = spanned.toString().trim()
            append(EmojiParser.parse(rawText))

            if (spanned is android.text.Spanned) {
                val spans =
                    spanned.getSpans(0, spanned.length, android.text.style.URLSpan::class.java)
                spans.forEach { span ->
                    val start = spanned.getSpanStart(span)
                    val end = spanned.getSpanEnd(span)

                    addStringAnnotation("URL", span.url, start, end)
                    addStyle(
                        SpanStyle(
                            color = Color(0xFF1E88E5),
                            textDecoration = TextDecoration.Underline
                        ),
                        start, end
                    )
                }
            }
        }

        return CommentContent(text = annotatedText, images = extractedImages)
    }
}