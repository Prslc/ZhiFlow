package com.prslc.zhiflow.data.remote.parser

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.core.utils.JsonHelper
import com.prslc.zhiflow.data.model.content.Card
import com.prslc.zhiflow.data.model.content.CardExtraInfo
import com.prslc.zhiflow.data.model.content.Mark
import com.prslc.zhiflow.data.model.content.Paragraph
import com.prslc.zhiflow.data.model.content.Segment
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder
import com.prslc.zhiflow.data.remote.parser.engine.FormulaHandler
import com.prslc.zhiflow.data.remote.parser.engine.TableParser
import com.prslc.zhiflow.data.remote.parser.model.ProcessedText
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement

@Immutable
object ContentParser {

    /**
     * Page furniture rather than content: dropped here so the parsed body is the body everywhere.
     */
    private val DROPPED_CARD_TYPES = setOf("reward_tail_truncate", "free_column_card")

    /**
     * Transform raw API segments into renderable [RichTextElement] list.
     *
     * Handles paragraph, heading, list, blockquote, table, card, image, code block,
     * reference block, and horizontal rule segment types.
     */
    fun transform(
        segments: List<Segment>,
    ): List<RichTextElement> {
        return segments.flatMap { segment ->
            when (segment.type) {
                "paragraph" -> processParagraph(segment.paragraph)

                "heading" -> segment.heading?.let {
                    val p = parseContent(it.text, it.marks)
                    listOf(RichTextElement.Heading(p.content, p.inlineMetas, it.level))
                } ?: emptyList()

                "list_node" -> segment.listNode?.let { listNode ->
                    // One counter per list, not per call: a new list_node means the list was
                    // interrupted, and the API gives no signal that the numbering carries on.
                    // Sharing one across a parsing chunk instead made the result depend on where
                    // the chunk boundary fell.
                    val counter = OrderedListCounter()
                    listNode.items.map { item ->
                        val p = parseContent(item.text, item.marks)
                        RichTextElement.BulletItem(
                            p.content, p.inlineMetas, item.indentLevel,
                            listNode.type == "ordered", counter.next(item.indentLevel)
                        )
                    }
                } ?: emptyList()

                "blockquote" -> segment.blockquote?.let {
                    val p = parseContent(it.text, it.marks)
                    listOf(RichTextElement.Blockquote(p.content, p.inlineMetas))
                } ?: emptyList()

                "table" -> segment.table?.let {
                    listOf(TableParser.parse(it) { text ->
                        parseContent(text, emptyList())
                    })
                } ?: emptyList()

                "card" -> parseCard(segment.card)
                "image" -> segment.image?.let { listOf(RichTextElement.Image(it)) } ?: emptyList()
                "code_block" -> segment.codeBlock?.let {
                    listOf(RichTextElement.Code(it.content, it.language))
                } ?: emptyList()

                "reference_block" -> segment.referenceBlock?.let { block ->
                    val items = block.items.map {
                        val p = parseContent(it.text, it.marks)
                        RichTextElement.ParsedText(p.content, p.inlineMetas)
                    }
                    listOf(RichTextElement.Reference(items))
                } ?: emptyList()

                "hr" -> listOf(RichTextElement.Divider)
                else -> emptyList()
            }
        }
    }

    private fun parseContent(
        rawText: String,
        marks: List<Mark>,
        paragraphId: String? = null
    ): ProcessedText {
        return AnnotatedStringBuilder.build(
            rawText = rawText,
            marks = marks,
            segmentLikes = segmentLikeTargets(rawText, marks, paragraphId),
            onFormulaFound = { mark, pos ->
                mark.formula?.let {
                    FormulaHandler.prepareInlineMeta(it, pos)
                }
            }
        )
    }

    /**
     * Collapse the `seg_like` marks of one paragraph into one target per range.
     *
     * The API sends the range twice — the shared record and, once the reader has liked it, the
     * reader's own record under a second key. Counts only live on the shared one, and the id to
     * unlike with only on the reader's. Both the liked flag and that id are therefore read from the
     * reader's record alone: taking the flag from the shared one could report a like that carries
     * no id to undo it with.
     */
    private fun segmentLikeTargets(
        rawText: String,
        marks: List<Mark>,
        paragraphId: String?
    ): List<SegmentLikeTarget> = marks
        .filter { it.type == "seg_like" }
        .groupBy { it.start to it.end }
        .mapNotNull { (range, group) ->
            val shared = group.firstNotNullOfOrNull { it.segLike }
            val mine = group.firstNotNullOfOrNull { it.masterSegLike }
            val segId = shared?.segIds?.firstOrNull() ?: mine?.segIds?.firstOrNull()
                ?: return@mapNotNull null

            val (start, end) = range
            val from = start.coerceIn(0, rawText.length)
            val to = end.coerceIn(from, rawText.length)

            SegmentLikeTarget(
                segId = segId,
                paragraphId = paragraphId,
                rawStart = from,
                rawEnd = to,
                text = rawText.substring(from, to),
                mySegId = mine?.segIds?.firstOrNull(),
                isLiked = mine?.isLike == true,
                likeCount = shared?.count ?: mine?.count ?: 0,
                commentCount = shared?.commentCount ?: 0,
            )
        }

    private fun processParagraph(
        paragraph: Paragraph?,
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
            val processed = parseContent(rawText, marks, paragraph.pid)
            return listOf(
                RichTextElement.ParsedText(
                    processed.content,
                    processed.inlineMetas,
                    processed.segmentLikes,
                )
            )
        }

        val elements = mutableListOf<RichTextElement>()
        var lastIndex = 0

        blockFormulaMarks.forEach { mark ->
            if (mark.start > lastIndex) {
                val subText = rawText.substring(lastIndex, mark.start)
                if (subText.isNotBlank() && subText != "\n") {
                    val subMarks = marks.filter { it.start >= lastIndex && it.end <= mark.start }
                        .map { it.copy(start = it.start - lastIndex, end = it.end - lastIndex) }
                    val processed = parseContent(subText, subMarks, paragraph.pid)
                    elements.add(
                        RichTextElement.ParsedText(
                            processed.content,
                            processed.inlineMetas,
                            processed.segmentLikes,
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
                val processed = parseContent(subText, subMarks, paragraph.pid)
                elements.add(
                    RichTextElement.ParsedText(
                        processed.content,
                        processed.inlineMetas,
                        processed.segmentLikes,
                    )
                )
            }
        }

        return elements
    }

    private fun parseCard(card: Card?) = card?.let {
        if (it.cardType in DROPPED_CARD_TYPES) return@let emptyList()

        val extra = JsonHelper.parseExtraInfo(it.extraInfo)

        if (it.cardType == "matrix-image-card") {
            extra?.imageList?.images?.mapNotNull { img ->
                val url = img.originalUrl ?: img.url
                if (url.isBlank()) null
                else RichTextElement.Image(
                    ZhihuImage(
                        urls = listOf(url),
                        width = img.originalWidth ?: img.width,
                        height = img.originalHeight ?: img.height,
                        description = "",
                        isGif = img.suffix == "gif",
                    )
                )
            } ?: emptyList()
        } else {
            listOf(
                RichTextElement.Card(
                    cardType = it.cardType,
                    title = it.title ?: extra?.title ?: "No title",
                    url = it.url ?: extra?.url ?: "",
                    cover = extra?.cover?.takeIf { c -> c.isNotBlank() } ?: it.cover,
                    desc = JsonHelper.cleanHtmlDesc(extra?.desc),
                    contentType = it.contentType ?: extra?.contentType
                ))
        }
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
