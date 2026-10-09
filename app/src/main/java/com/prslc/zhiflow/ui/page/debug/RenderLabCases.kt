package com.prslc.zhiflow.ui.page.debug

import android.net.Uri
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

/** The LaTeX the cases draw, each of them measured in [FORMULA_ASPECTS]. */
private const val LATEX_PYTHAGORAS = "a^2+b^2=c^2"
private const val LATEX_FRACTION = "\\frac{a}{b}"
private const val LATEX_PRODUCT =
    "\\displaystyle C_{m\\times k}=A_{m\\times n}\\cdot B_{n\\times k}"
private const val LATEX_TWO_PRODUCTS =
    "\\displaystyle C_{m\\times k}=A_{m\\times n}\\cdot B_{n\\times k},\\quad" +
        " D_{p\\times q}=E_{p\\times r}\\cdot F_{r\\times q}"
private const val LATEX_INTEGRAL = "\\displaystyle \\int_0^1 f(x)\\,dx"
private const val LATEX_SQUARE = "x^2"
private const val LATEX_MASS_ENERGY = "E=mc^2"

/**
 * How wide each case's formula is drawn, as a multiple of its height.
 *
 * Measured off the endpoint's own SVG -- its width and height, both in ex -- rather than made up:
 * the image is fitted to the box it is given, so a box of the wrong proportions comes out
 * stretched. The API sends a size that matches, for the same reason. Measuring one is a `curl` of
 * its url and a division; a formula drawn here without that measurement stops the page rather than
 * being drawn at a guessed shape.
 */
private val FORMULA_ASPECTS = mapOf(
    LATEX_PYTHAGORAS to 4.34f,
    LATEX_FRACTION to 0.43f,
    LATEX_PRODUCT to 8.39f,
    LATEX_TWO_PRODUCTS to 15.30f,
    LATEX_INTEGRAL to 1.82f,
    LATEX_SQUARE to 0.89f,
    LATEX_MASS_ENERGY to 3.35f,
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
 * The url a formula's bitmap is rendered from. The size is ours to pick, which is the point of the
 * lab: the same formula at 13dp and at 120dp are two different things to look at. A case lays out
 * correctly whether or not the bitmap arrives.
 */
private fun equationUrl(latex: String) = "https://www.zhihu.com/equation?tex=${Uri.encode(latex)}"

/** A formula of [height] dp, as wide as its own shape makes it. */
private fun formula(latex: String, height: Int) = Formula(
    content = latex,
    imgUrl = equationUrl(latex),
    width = (height * (FORMULA_ASPECTS[latex] ?: error("unmeasured formula: $latex"))).roundToInt(),
    height = height,
)

/** A paragraph whose single inline formula sits at [start], the text carrying the placeholder. */
private fun formulaMark(text: String, start: Int, latex: String, height: Int) = Mark(
    type = "formula",
    start = start,
    end = start + FORMULA_TEXT.length,
    formula = formula(latex, height),
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
                        LATEX_PYTHAGORAS,
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
                        LATEX_FRACTION,
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
                        LATEX_PRODUCT,
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
                    formula = formula(LATEX_TWO_PRODUCTS, params.formulaHeight),
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
                            LATEX_INTEGRAL,
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
        listOf(formulaMark(text, text.indexOf(PLACEHOLDER), LATEX_SQUARE, it))
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
                content = "fun main() {\n    println(\"a code block with a language\")\n}",
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
                                    LATEX_MASS_ENERGY,
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
