package com.prslc.zhiflow.parser

import android.text.Html
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

data class CommentContent(
    val text: String,
    val images: List<ZhihuImage>
)

sealed interface RichTextElement {
    data class Text(val content: AnnotatedString) : RichTextElement
    data class Heading(val content: AnnotatedString, val level: Int = 2) : RichTextElement
    data class Image(val data: ZhihuImage) : RichTextElement
    data class BulletItem(
        val content: AnnotatedString,
        val level: Int,
        val isOrdered: Boolean,
        val index: Int = 0  // ordered list
    ) : RichTextElement
    data class FormulaBlock(val data: Formula) : RichTextElement
    data class Blockquote(val content: AnnotatedString) : RichTextElement
    data class Code(val code: String, val lang: String?) : RichTextElement
    data class Reference(val items: List<AnnotatedString>) : RichTextElement
    data class Table(
        val rows: Int,
        val cols: Int,
        val cells: List<String>,
        val hasHeader: Boolean
    ) : RichTextElement
    data object Divider : RichTextElement
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
                        listOf(RichTextElement.Table(
                            rows = it.rowCount,
                            cols = it.columnCount,
                            cells = it.cells,
                            hasHeader = it.hasHeadRow
                        ))
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

        if (paragraph.text.trim() == "---") return listOf(RichTextElement.Divider)

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
            append(rawText)
            marks.forEach { mark ->
                val start = mark.start.coerceIn(0, rawText.length)
                val end = mark.end.coerceIn(0, rawText.length)
                if (start >= end) return@forEach

                when (mark.type) {
                    "bold" -> addStyle(SpanStyle(fontWeight = FontWeight.ExtraBold), start, end)
                    "italic" -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    "strikethrough" -> addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                        start,
                        end
                    )
                    "code" -> addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Gray.copy(alpha = 0.1f)
                        ), start, end
                    )

                    "reference" -> {
                        addStyle(
                            style = SpanStyle(
                                fontSize = 12.sp,
                                baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E88E5)
                            ),
                            start = start,
                            end = end
                        )
                        mark.reference?.title?.let { title ->
                            addStringAnnotation(
                                tag = "REF_TITLE",
                                annotation = title,
                                start = start,
                                end = end
                            )
                        }
                    }
                    // Do not parse entity_word, as this is a quick search.
                    "link" -> {
                        val url = mark.link?.href ?: mark.entityWord?.url
                        if (!url.isNullOrEmpty()) {
                            addStringAnnotation(
                                tag = "URL",
                                annotation = url,
                                start = start,
                                end = end
                            )
                            addStyle(SpanStyle(color = Color(0xFF1E88E5)), start, end)
                        }
                    }

                    "formula" -> {
                        mark.formula?.imgUrl?.let { url ->
                            addStringAnnotation(
                                tag = "INLINE_FORMULA",
                                annotation = url,
                                start = start,
                                end = end
                            )
                        }
                    }
                }
            }
        }
    }

    fun parseCommentHtml(html: String): CommentContent {
        val extractedImages = mutableListOf<ZhihuImage>()

        val aTagRegex = """<a[^>]+class="[^"]*comment_img[^"]*"[^>]*>.*?</a>""".toRegex()
        val hrefRegex = """href="([^"]+)"""".toRegex()
        val widthRegex = """data-width="(\d+)"""".toRegex()
        val heightRegex = """data-height="(\d+)"""".toRegex()

        var processedHtml = html

        aTagRegex.findAll(html).forEach { match ->
            val fullTag = match.value

            val url = hrefRegex.find(fullTag)?.groupValues?.get(1) ?: ""
            val w = widthRegex.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val h = heightRegex.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0

            if (url.isNotEmpty()) {
                extractedImages.add(ZhihuImage(urls = listOf(url), width = w, height = h, isGif = false, description = ""))
            }

            processedHtml = processedHtml.replace(fullTag, "")
        }

        val cleanText = Html.fromHtml(processedHtml, Html.FROM_HTML_MODE_COMPACT)
            .toString()
            .trim()

        return CommentContent(
            text = cleanText,
            images = extractedImages
        )
    }
}