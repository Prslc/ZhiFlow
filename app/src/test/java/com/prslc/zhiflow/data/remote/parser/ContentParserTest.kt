package com.prslc.zhiflow.data.remote.parser

import com.prslc.zhiflow.data.model.content.Blockquote
import com.prslc.zhiflow.data.model.content.Card
import com.prslc.zhiflow.data.model.content.CodeBlock
import com.prslc.zhiflow.data.model.content.Formula
import com.prslc.zhiflow.data.model.content.Heading
import com.prslc.zhiflow.data.model.content.ListItem
import com.prslc.zhiflow.data.model.content.ListNode
import com.prslc.zhiflow.data.model.content.Mark
import com.prslc.zhiflow.data.model.content.Paragraph
import com.prslc.zhiflow.data.model.content.ReferenceBlock
import com.prslc.zhiflow.data.model.content.ReferenceItem
import com.prslc.zhiflow.data.model.content.Segment
import com.prslc.zhiflow.data.model.content.SegmentLike
import com.prslc.zhiflow.data.model.content.Table
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the API's segments turn into.
 *
 * The parser is the one long stretch of this app with no framework under it and no picture of its
 * own, which makes it the piece a test can hold still -- and the piece where a mistake is silent.
 * A body that parses into the wrong shape still draws: a lost offset moves a formula, a reset list
 * renumbers itself, a card falls back to a placeholder title. Nothing downstream fails loudly, so
 * nothing downstream catches it either.
 */
class ContentParserTest {

    /**
     * The server's own placeholder for an inline formula, which is what a paragraph holds in the
     * text where its bitmap goes. It is data the API sends rather than prose, and the parser reads
     * it literally at `ContentParser.processParagraph`.
     */
    private val formulaPlaceholder = "[公式]"

    private fun paragraph(
        text: String,
        marks: List<Mark> = emptyList(),
        pid: String? = null,
    ) = Segment(type = "paragraph", paragraph = Paragraph(text = text, marks = marks, pid = pid))

    private fun formulaMark(start: Int, end: Int, latex: String, width: Int = 84, height: Int = 20) =
        Mark(
            type = "formula",
            start = start,
            end = end,
            formula = Formula(content = latex, imgUrl = "https://example.test/f.jpg", width = width, height = height),
        )

    // Dispatch

    @Test
    fun `each kind of segment becomes the element it names`() {
        val elements = ContentParser.transform(
            listOf(
                Segment(type = "heading", heading = Heading(text = "A heading", level = 3)),
                paragraph("A paragraph."),
                Segment(type = "hr"),
                Segment(
                    type = "image",
                    image = ZhihuImage(
                        urls = listOf("https://example.test/i.jpg"),
                        width = 800,
                        height = 600,
                        description = "",
                        isGif = false,
                    ),
                ),
                Segment(type = "code_block", codeBlock = CodeBlock("val x = 1", "kotlin")),
            ),
        )

        assertTrue(elements[0] is RichTextElement.Heading)
        assertEquals(3, (elements[0] as RichTextElement.Heading).level)
        assertTrue(elements[1] is RichTextElement.ParsedText)
        assertEquals(RichTextElement.Divider, elements[2])
        assertTrue(elements[3] is RichTextElement.Image)
        assertTrue(elements[4] is RichTextElement.Code)
    }

    @Test
    fun `a kind the parser does not know is dropped, not drawn as something else`() {
        assertEquals(emptyList<RichTextElement>(), ContentParser.transform(listOf(Segment(type = "video"))))
    }

    @Test
    fun `a segment whose body is missing is dropped`() {
        // The API says `paragraph` and then sends nothing under it. Reading the type alone and
        // reaching into the body would throw here, in a parse with no way to report it.
        assertEquals(emptyList<RichTextElement>(), ContentParser.transform(listOf(Segment(type = "paragraph"))))
        assertEquals(emptyList<RichTextElement>(), ContentParser.transform(listOf(Segment(type = "heading"))))
        assertEquals(emptyList<RichTextElement>(), ContentParser.transform(listOf(Segment(type = "table"))))
    }

    @Test
    fun `a table keeps the shape the server sent`() {
        val elements = ContentParser.transform(
            listOf(
                Segment(
                    type = "table",
                    table = Table(
                        cells = listOf("a", "b", "c", "d"),
                        columnCount = 2,
                        rowCount = 2,
                        hasHeadRow = true,
                    ),
                ),
            ),
        )

        val table = elements.single() as RichTextElement.Table
        assertEquals(2, table.cols)
        assertEquals(2, table.rows)
        assertTrue(table.hasHeader)
        assertEquals(listOf("a", "b", "c", "d"), table.cells.map { it.content.text })
    }

    // Block formulas

    @Test
    fun `a paragraph that is nothing but a formula becomes the formula alone`() {
        val elements = ContentParser.transform(
            listOf(
                paragraph(
                    text = formulaPlaceholder,
                    marks = listOf(formulaMark(0, formulaPlaceholder.length, "n^2")),
                ),
            ),
        )

        assertEquals(1, elements.size)
        assertTrue(elements.single() is RichTextElement.FormulaBlock)
    }

    @Test
    fun `an aligned formula splits its paragraph into the text around it`() {
        val text = "Before $formulaPlaceholder after."
        val start = text.indexOf(formulaPlaceholder)
        val elements = ContentParser.transform(
            listOf(
                paragraph(
                    text = text,
                    marks = listOf(formulaMark(start, start + formulaPlaceholder.length, "\\begin{aligned}a\\end{aligned}")),
                ),
            ),
        )

        assertEquals(3, elements.size)
        assertEquals("Before ", (elements[0] as RichTextElement.ParsedText).content.text)
        assertTrue(elements[1] is RichTextElement.FormulaBlock)
        assertEquals(" after.", (elements[2] as RichTextElement.ParsedText).content.text)
    }

    @Test
    fun `an inline formula in the text after a block formula keeps its place in that text`() {
        // The sub-text is parsed on its own, so the marks handed to it are rebased. A formula that
        // kept its offset into the whole paragraph would be keyed to a position that no longer
        // exists in the text it is drawn over: here it sits 6 characters into the tail, not 15 into
        // the paragraph.
        val block = "\\begin{aligned}a\\end{aligned}"
        val inline = "x^2"
        val text = "text $formulaPlaceholder then $formulaPlaceholder end"
        val blockStart = text.indexOf(formulaPlaceholder)
        val inlineStart = text.lastIndexOf(formulaPlaceholder)

        val elements = ContentParser.transform(
            listOf(
                paragraph(
                    text = text,
                    marks = listOf(
                        formulaMark(blockStart, blockStart + formulaPlaceholder.length, block),
                        formulaMark(inlineStart, inlineStart + formulaPlaceholder.length, inline),
                    ),
                ),
            ),
        )

        val tail = elements.last() as RichTextElement.ParsedText
        // What an inline formula stands in as is its LaTeX, so that is what the built text reads
        // where the placeholder was.
        assertEquals(" then $inline end", tail.content.text)
        assertEquals(listOf("f_6_${inline.hashCode()}"), tail.inlineMetas.map { it.inlineId })
    }

    @Test
    fun `a mark reaching past its paragraph is clamped rather than cut out of it`() {
        // The offsets are the server's, measured against text this app has already had. One that
        // reaches past the end has to be clamped the way a passage's offsets are: `substring`
        // throws, and a throw out of a mapper is not a `Result` in this app -- it leaves the
        // coroutine, and on a device it takes the process with it.
        val overrun = 999
        val text = "before $formulaPlaceholder after"
        val start = text.indexOf(formulaPlaceholder)
        val block = "\\begin{aligned}a\\end{aligned}"

        val elements = ContentParser.transform(
            listOf(
                paragraph(
                    text = text,
                    marks = listOf(
                        // An end running past the paragraph, which is where the index the next mark
                        // is measured against comes from.
                        formulaMark(start, overrun, block),
                        // And a start past the end of the paragraph outright.
                        formulaMark(overrun + 1, overrun * 2, block),
                    ),
                ),
            ),
        )

        // Neither mark can be placed, so neither of them splits the paragraph. The formulas are
        // still drawn, and the text before the first of them is where it was.
        assertEquals(2, elements.count { it is RichTextElement.FormulaBlock })
        assertEquals("before ", (elements.first() as RichTextElement.ParsedText).content.text)
    }

    @Test
    fun `a blank run between two formulas is not drawn`() {
        val block = "\\begin{aligned}a\\end{aligned}"
        val text = "$formulaPlaceholder\n$formulaPlaceholder"
        val second = text.lastIndexOf(formulaPlaceholder)

        val elements = ContentParser.transform(
            listOf(
                paragraph(
                    text = text,
                    marks = listOf(
                        formulaMark(0, formulaPlaceholder.length, block),
                        formulaMark(second, second + formulaPlaceholder.length, block),
                    ),
                ),
            ),
        )

        assertEquals("the newline between them is not a paragraph", 2, elements.size)
        assertTrue(elements.all { it is RichTextElement.FormulaBlock })
    }

    @Test
    fun `a formula with no LaTeX is left out rather than stood in for empty`() {
        // The bitmap is rendered from the LaTeX, so the LaTeX is also what stands in the text for
        // it -- and an empty stand-in is refused outright. The placeholder goes with the mark,
        // leaving the gap the formula would have filled.
        val text = "before $formulaPlaceholder after"
        val start = text.indexOf(formulaPlaceholder)

        val elements = ContentParser.transform(
            listOf(paragraph(text = text, marks = listOf(formulaMark(start, start + formulaPlaceholder.length, "")))),
        )

        val only = elements.single() as RichTextElement.ParsedText
        assertEquals("before  after", only.content.text)
        assertTrue(only.inlineMetas.isEmpty())
    }

    // Lists

    private fun listNode(type: String, levels: List<Int>) = Segment(
        type = "list_node",
        listNode = ListNode(
            type = type,
            items = levels.map { ListItem(text = "item", indentLevel = it) },
        ),
    )

    @Test
    fun `an ordered list numbers each level on its own`() {
        val items = ContentParser.transform(listOf(listNode("ordered", listOf(0, 0, 1, 1, 0))))
            .map { it as RichTextElement.BulletItem }

        assertTrue(items.all { it.isOrdered })
        assertEquals(listOf(1, 2, 1, 2, 3), items.map { it.index })
    }

    @Test
    fun `going back out drops the deeper numbering, so it starts over on the way in`() {
        // The counter keeps one count per level and forgets everything deeper than the level being
        // numbered. Without that, a second nest would carry on from the first.
        val items = ContentParser.transform(listOf(listNode("ordered", listOf(0, 1, 0, 1))))
            .map { it as RichTextElement.BulletItem }

        assertEquals(listOf(1, 1, 2, 1), items.map { it.index })
    }

    @Test
    fun `a second list starts over`() {
        // One counter per list rather than per parse: sharing one across two lists -- or across the
        // chunks of one body -- made the numbering depend on where the boundary fell.
        val items = ContentParser.transform(
            listOf(listNode("ordered", listOf(0, 0)), listNode("ordered", listOf(0))),
        ).map { it as RichTextElement.BulletItem }

        assertEquals(listOf(1, 2, 1), items.map { it.index })
    }

    @Test
    fun `an unordered list is not numbered`() {
        val items = ContentParser.transform(listOf(listNode("unordered", listOf(0, 0))))
            .map { it as RichTextElement.BulletItem }

        assertTrue(items.none { it.isOrdered })
    }

    @Test
    fun `a list item keeps its indent level`() {
        val items = ContentParser.transform(listOf(listNode("unordered", listOf(0, 1, 2))))
            .map { it as RichTextElement.BulletItem }

        assertEquals(listOf(0, 1, 2), items.map { it.level })
    }

    // References

    @Test
    fun `a reference block keeps its items in order`() {
        val elements = ContentParser.transform(
            listOf(
                Segment(
                    type = "reference_block",
                    referenceBlock = ReferenceBlock(
                        items = listOf(ReferenceItem(text = "first"), ReferenceItem(text = "second")),
                    ),
                ),
            ),
        )

        val reference = elements.single() as RichTextElement.Reference
        assertEquals(listOf("first", "second"), reference.items.map { it.content.text })
    }

    // Cards

    private fun card(type: String, title: String? = null, extraInfo: String? = null) =
        Segment(type = "card", card = Card(cardType = type, title = title, extraInfo = extraInfo))

    @Test
    fun `a card the app has decided not to draw is dropped`() {
        assertEquals(emptyList<RichTextElement>(), ContentParser.transform(listOf(card("reward_tail_truncate"))))
    }

    @Test
    fun `a card with no title of its own is given one rather than drawn blank`() {
        val drawn = ContentParser.transform(listOf(card("link-card", title = "")))
            .single() as RichTextElement.Card

        assertEquals("No title", drawn.title)
    }

    @Test
    fun `a link card keeps the title and kind it came with`() {
        val drawn = ContentParser.transform(
            listOf(card("link-card", title = "A parse tree", extraInfo = null)),
        ).single() as RichTextElement.Card

        assertEquals("A parse tree", drawn.title)
        assertEquals("link-card", drawn.cardType)
    }

    @Test
    fun `a column card that cannot be read is dropped whole`() {
        // Both halves of it come out of `extra_info`: with nothing readable there is no column to
        // name and nowhere for a tap to go.
        assertEquals(
            emptyList<RichTextElement>(),
            ContentParser.transform(listOf(card("free_column_card", extraInfo = "not json"))),
        )
        assertEquals(
            emptyList<RichTextElement>(),
            ContentParser.transform(listOf(card("free_column_card", extraInfo = null))),
        )
    }

    // Passages

    private fun segLike(segIds: List<String>, count: Int = 0, isLike: Boolean = false) =
        SegmentLike(segIds = segIds, count = count, isLike = isLike)

    private fun quote(vararg marks: Mark) = Segment(
        type = "blockquote",
        blockquote = Blockquote(text = "A passage worth marking.", marks = marks.toList(), pid = "p1"),
    )

    @Test
    fun `the two records of one passage collapse into a single target`() {
        val shared = Mark(type = "seg_like", start = 0, end = 7, segLike = segLike(listOf("s1"), count = 12))
        val mine = Mark(
            type = "seg_like",
            start = 0,
            end = 7,
            masterSegLike = segLike(listOf("s1"), count = 12, isLike = true),
        )

        val likes = (ContentParser.transform(listOf(quote(shared, mine))).single() as RichTextElement.Blockquote)
            .segmentLikes

        assertEquals(1, likes.size)
        assertEquals("A passa", likes.single().text)
    }

    @Test
    fun `the count comes from the shared record and the like from the reader's own`() {
        // Counts only live on the record everyone sees, and the id to unlike with only on the
        // reader's. Reading the flag from the shared one would report a like with nothing to
        // undo it.
        val shared = Mark(type = "seg_like", start = 0, end = 7, segLike = segLike(listOf("s1"), count = 12))
        val mine = Mark(
            type = "seg_like",
            start = 0,
            end = 7,
            masterSegLike = segLike(listOf("mine"), count = 0, isLike = true),
        )

        val like = (ContentParser.transform(listOf(quote(shared, mine))).single() as RichTextElement.Blockquote)
            .segmentLikes.single()

        assertEquals(12, like.likeCount)
        assertTrue(like.isLiked)
        assertEquals("s1", like.segId)
        assertEquals("mine", like.mySegId)
    }

    @Test
    fun `a passage the reader has not liked carries the flag and no id of their own`() {
        val shared = Mark(type = "seg_like", start = 0, end = 7, segLike = segLike(listOf("s1"), count = 3))

        val like = (ContentParser.transform(listOf(quote(shared))).single() as RichTextElement.Blockquote)
            .segmentLikes.single()

        assertTrue(!like.isLiked)
        assertNull(like.mySegId)
        assertEquals(3, like.likeCount)
    }

    @Test
    fun `a passage with no id at all is dropped`() {
        // A range neither record names cannot be liked or reported on, so there is nothing for a
        // bubble to open.
        val nameless = Mark(type = "seg_like", start = 0, end = 7, segLike = SegmentLike())
        val quote = ContentParser.transform(listOf(quote(nameless))).single() as RichTextElement.Blockquote

        assertTrue(quote.segmentLikes.isEmpty())
    }

    @Test
    fun `a range reaching past the text is clamped to it`() {
        // Offsets come from the server against text this app has already trimmed.
        val overrun = Mark(
            type = "seg_like",
            start = 0,
            end = 999,
            segLike = segLike(listOf("s1"), count = 1),
        )

        val like = (ContentParser.transform(listOf(quote(overrun))).single() as RichTextElement.Blockquote)
            .segmentLikes.single()

        assertEquals("A passage worth marking.", like.text)
    }

    @Test
    fun `a passage carries the host it was marked in`() {
        // A like is addressed by the range and by the id of the host it was marked in, so a target
        // that lost the pid is one nothing could be sent about.
        val mark = Mark(type = "seg_like", start = 0, end = 7, segLike = segLike(listOf("s1"), count = 1))

        val fromParagraph = ContentParser.transform(listOf(paragraph("A passage.", listOf(mark), pid = "p1")))
            .single() as RichTextElement.ParsedText

        assertEquals("p1", fromParagraph.segmentLikes.single().paragraphId)
        assertEquals("A passa", fromParagraph.segmentLikes.single().text)
    }
}
