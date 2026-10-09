package com.prslc.zhiflow.ui.page.debug

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.content.Blockquote
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
import com.prslc.zhiflow.data.model.content.Table
import kotlin.math.roundToInt

/**
 * What the server puts in the text where a formula goes: the four characters an inline formula's
 * mark covers, and, on its own, the whole text of a display formula's paragraph.
 */
private const val FORMULA_TEXT = "[公式]"

private const val PLACEHOLDER = FORMULA_TEXT

/**
 * A formula as the API sends it: its LaTeX, the address of the bitmap the server rendered for it,
 * and the dp box that bitmap was rendered at.
 */
@Immutable
private data class LabFormula(
    val content: String,
    val url: String,
    val width: Int,
    val height: Int,
)

/**
 * The formulas the cases draw, taken from a capture of an answer.
 *
 * The lab uses captured addresses rather than ones it builds from the equation endpoint, because a
 * formula's box is the API's to decide: the bitmap is three times the dp size, transparent, with
 * its ink centred in the box. An address built from the endpoint answers with a box tight around
 * the ink, which is a different picture, and a case would be showing something no reader sees.
 */
private val SMALL_FORMULA = LabFormula(
    content = "n^2",
    url = "https://picx.zhimg.com/v2-4620adc1c46d46a7528f1a5e7c936d5a.jpg" +
        "?source=7e7ef6e2&needBackground=1",
    width = 18,
    height = 20,
)

private val TINY_FORMULA = LabFormula(
    content = "\\varepsilon",
    url = "https://picx.zhimg.com/v2-460a6ba3eeee65b706f3f1c53993f2dd.jpg" +
        "?source=7e7ef6e2&needBackground=1",
    width = 8,
    height = 13,
)

private val WIDEISH_FORMULA = LabFormula(
    content = "n \\times n = n^2",
    url = "https://picx.zhimg.com/v2-bdfa9d725446079f8ab0e42ccb8d855f.jpg" +
        "?source=7e7ef6e2&needBackground=1",
    width = 84,
    height = 20,
)

private val TALL_FORMULA = LabFormula(
    content = "\\begin{aligned} T(n) = 8 T\\left(\\frac{n}{2}\\right) + " +
        "4\\mathcal{O}(n^2) \\end{aligned}",
    url = "https://pic1.zhimg.com/v2-f66cef394f15a55638e19eaa69694e4d.jpg" +
        "?source=7e7ef6e2&needBackground=1",
    width = 202,
    height = 36,
)

private val TALLER_FORMULA = LabFormula(
    content = "\\begin{aligned} C_{11} &= A_{11}B_{11} + A_{12}B_{21} \\\\ " +
        "C_{12} &= A_{11}B_{12} + A_{12}B_{22} \\\\ C_{21} &= A_{21}B_{11} + A_{22}B_{21} \\\\ " +
        "C_{22} &= A_{21}B_{12} + A_{22}B_{22} \\end{aligned}",
    url = "https://picx.zhimg.com/v2-70897a1a53e0fb13afcfc2d38fbb98f8.jpg" +
        "?source=7e7ef6e2&needBackground=1",
    width = 186,
    height = 89,
)

private val OVERWIDE_FORMULA = LabFormula(
    content = "\\begin{aligned} \\omega = \\inf \\{ \\tau \\mid R(\\langle n, n, n \\rangle) = " +
        "\\mathcal{O}(n^\\tau) \\} = \\inf \\{ \\tau \\mid \\underline{R}(\\langle n, n, n " +
        "\\rangle) = \\mathcal{O}(n^\\tau) \\} \\end{aligned}",
    url = "https://pica.zhimg.com/v2-0ba3b251bf1bf54156f9570337453269.jpg" +
        "?source=7e7ef6e2&needBackground=1",
    width = 498,
    height = 26,
)

/**
 * The quantities the cases are built from, and the ones a rendering bug has turned on so far: how
 * tall a formula is against the line holding it, which line of a paragraph it lands on, how many
 * columns a table is asked for.
 *
 * @param formulaHeight The dp height of every inline formula the cases draw.
 * @param formulaLine Which line of a paragraph, quote or list item the formula sits on.
 * @param quoteLines How many lines the quoted case has.
 * @param tableCols How many columns the table case has.
 * @param cellChars How many characters each body cell of the table holds.
 * @param listDepth The indent level both lists are drawn at.
 */
@Immutable
data class LabParams(
    val formulaHeight: Int = 56,
    val formulaLine: Int = 1,
    val quoteLines: Int = 3,
    val tableCols: Int = 2,
    val cellChars: Int = 8,
    val listDepth: Int = 1,
)

/**
 * One case: the raw segments to render, and what went into them, so the page can print the
 * parameters above the thing they produced.
 *
 * @param title What the case shows.
 * @param note The format its parameters are printed with, or null when it has none.
 * @param noteArgs The arguments for [note].
 * @param segments The segments, in the shape the API sends them.
 */
@Immutable
data class LabCase(
    @StringRes val title: Int,
    @StringRes val note: Int? = null,
    val noteArgs: List<Any> = emptyList(),
    val segments: List<Segment>,
)

/** The cases, in the order the page draws them. */
fun labCases(params: LabParams): List<LabCase> = listOf(
    headingsCase(params),
    paragraphCase(params),
    blockFormulaCase(params),
    wideFormulaCase(params),
    quoteCase(params),
    listCase(params),
    tableCase(params),
    codeCase(),
    dividerCase(),
    referenceCase(params),
)

/**
 * A formula whose box is [height] dp tall, the width following the one the API rendered it at so
 * that the picture keeps its shape.
 */
private fun formula(basis: LabFormula, height: Int): Formula {
    val scale = height.toFloat() / basis.height
    return Formula(
        content = basis.content,
        imgUrl = basis.url,
        width = (basis.width * scale).roundToInt().coerceAtLeast(1),
        height = height,
    )
}

/** A paragraph whose single inline formula sits at [start], the text carrying the placeholder. */
private fun formulaMark(text: String, start: Int, basis: LabFormula, height: Int) = Mark(
    type = "formula",
    start = start,
    end = start + FORMULA_TEXT.length,
    formula = formula(basis, height),
)

private fun paragraph(text: String, marks: List<Mark>) =
    Segment(type = "paragraph", paragraph = Paragraph(text = text, marks = marks))

/** The text of a case: [lines] lines, the formula on [formulaLine], placeholder included. */
private fun linesWithFormula(lines: Int, formulaLine: Int): String =
    (1..lines).joinToString("\n") { index ->
        if (index == formulaLine) {
            "Line $index: an inline formula $PLACEHOLDER and the text that follows it."
        } else {
            "Line $index: here to show how one line's height is shared with the rest of the " +
                "paragraph."
        }
    }

private fun headingsCase(params: LabParams): LabCase {
    val segments = (1..4).map { level ->
        val text = "Heading $level with an inline formula $PLACEHOLDER"
        Segment(
            type = "heading",
            heading = Heading(
                text = text,
                level = level,
                marks = listOf(
                    formulaMark(
                        text,
                        text.indexOf(PLACEHOLDER),
                        SMALL_FORMULA,
                        params.formulaHeight,
                    )
                ),
            ),
        )
    }
    return LabCase(
        title = R.string.lab_case_headings,
        note = R.string.lab_note_formula,
        noteArgs = listOf(params.formulaHeight),
        segments = segments,
    )
}

private fun paragraphCase(params: LabParams): LabCase {
    val text = linesWithFormula(maxOf(params.formulaLine, 3), params.formulaLine)
    return LabCase(
        title = R.string.lab_case_paragraph,
        note = R.string.lab_note_formula_line,
        noteArgs = listOf(params.formulaHeight, params.formulaLine),
        segments = listOf(
            paragraph(
                text,
                listOf(
                    formulaMark(
                        text,
                        text.indexOf(PLACEHOLDER),
                        WIDEISH_FORMULA,
                        params.formulaHeight,
                    )
                ),
            )
        ),
    )
}

private fun blockFormulaCase(params: LabParams): LabCase = LabCase(
    title = R.string.lab_case_block_formula,
    note = R.string.lab_note_formula,
    noteArgs = listOf(params.formulaHeight),
    segments = listOf(
        paragraph(
            FORMULA_TEXT,
            listOf(
                Mark(
                    type = "formula",
                    start = 0,
                    end = FORMULA_TEXT.length,
                    formula = formula(
                        TALLER_FORMULA,
                        params.formulaHeight,
                    ),
                )
            ),
        )
    ),
)

private fun wideFormulaCase(params: LabParams): LabCase = LabCase(
    title = R.string.lab_case_wide_formula,
    note = R.string.lab_note_formula_width,
    noteArgs = listOf(params.formulaHeight),
    segments = listOf(
        paragraph(
            FORMULA_TEXT,
            listOf(
                Mark(
                    type = "formula",
                    start = 0,
                    end = FORMULA_TEXT.length,
                    formula = formula(OVERWIDE_FORMULA, params.formulaHeight),
                )
            ),
        )
    ),
)

private fun quoteCase(params: LabParams): LabCase {
    val formulaLine = minOf(params.formulaLine, params.quoteLines)
    val text = linesWithFormula(params.quoteLines, formulaLine)
    return LabCase(
        title = R.string.lab_case_quote,
        note = R.string.lab_note_quote,
        noteArgs = listOf(params.quoteLines, formulaLine),
        segments = listOf(
            Segment(
                type = "blockquote",
                blockquote = Blockquote(
                    text = text,
                    marks = listOf(
                        formulaMark(
                            text,
                            text.indexOf(PLACEHOLDER),
                            TALL_FORMULA,
                            params.formulaHeight,
                        )
                    ),
                ),
            )
        ),
    )
}

private fun listCase(params: LabParams): LabCase {
    val depth = params.listDepth
    val segments = listOf("unordered", "ordered").map { type ->
        val items = listOf(
            listItem(
                "An item whose first line carries the formula $PLACEHOLDER",
                depth,
                params.formulaHeight,
            ),
            listItem(
                "An item that only reaches its formula on the\nsecond line $PLACEHOLDER",
                depth,
                params.formulaHeight,
            ),
            listItem("A sub-item one level in", depth + 1),
        )
        Segment(type = "list_node", listNode = ListNode(type = type, items = items))
    }
    return LabCase(
        title = R.string.lab_case_list,
        note = R.string.lab_note_list,
        noteArgs = listOf(depth),
        segments = segments,
    )
}

private fun listItem(text: String, depth: Int, formulaHeight: Int? = null): ListItem = ListItem(
    text = text,
    indentLevel = depth,
    marks = formulaHeight?.let {
        listOf(formulaMark(text, text.indexOf(PLACEHOLDER), TINY_FORMULA, it))
    } ?: emptyList(),
)

private fun tableCase(params: LabParams): LabCase {
    val body = "x".repeat(params.cellChars)
    val cells = List(params.tableCols * TABLE_ROWS) { index ->
        if (index < params.tableCols) "Head ${index + 1}" else body
    }
    return LabCase(
        title = R.string.lab_case_table,
        note = R.string.lab_note_table,
        noteArgs = listOf(params.tableCols, TABLE_ROWS, params.cellChars),
        segments = listOf(
            Segment(
                type = "table",
                table = Table(
                    cells = cells,
                    columnCount = params.tableCols,
                    rowCount = TABLE_ROWS,
                    hasHeadRow = true,
                ),
            )
        ),
    )
}

private const val TABLE_ROWS = 3

private fun codeCase(): LabCase = LabCase(
    title = R.string.lab_case_code,
    segments = listOf(
        Segment(
            type = "code_block",
            codeBlock = CodeBlock(
                content = "/* What a cart is worth, in cents. */\n" +
                    "@JvmStatic\n" +
                    "fun price(items: List<Int>, tag: String = \"cart\"): Int {\n" +
                    "    // 3 items, at 1.5 each.\n" +
                    "    return if (items.size > 3) 12 else items.size\n" +
                    "}",
                language = "kotlin",
            ),
        ),
        Segment(
            type = "code_block",
            codeBlock = CodeBlock(
                content = "a code block with no language\nsecond line",
                language = null,
            ),
        ),
    ),
)

private fun dividerCase(): LabCase = LabCase(
    title = R.string.lab_case_divider,
    segments = listOf(Segment(type = "hr")),
)

private fun referenceCase(params: LabParams): LabCase {
    val text = "The first footnote, with an inline formula $PLACEHOLDER."
    return LabCase(
        title = R.string.lab_case_reference,
        note = R.string.lab_note_formula,
        noteArgs = listOf(params.formulaHeight),
        segments = listOf(
            Segment(
                type = "reference_block",
                referenceBlock = ReferenceBlock(
                    items = listOf(
                        ReferenceItem(
                            text = text,
                            marks = listOf(
                                formulaMark(
                                    text,
                                    text.indexOf(PLACEHOLDER),
                                    WIDEISH_FORMULA,
                                    params.formulaHeight,
                                )
                            ),
                        ),
                        ReferenceItem(text = "The second footnote, without a formula."),
                    ),
                ),
            )
        ),
    )
}
