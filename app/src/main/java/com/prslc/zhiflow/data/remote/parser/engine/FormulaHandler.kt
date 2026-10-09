package com.prslc.zhiflow.data.remote.parser.engine

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.content.Formula
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta

@Immutable
object FormulaHandler {
    /**
     * The meta an inline formula is drawn from, or null to leave it out of the text.
     *
     * A formula that arrived with no LaTeX is left out: its bitmap would have been rendered from
     * that LaTeX, and the text an inline formula stands for is the LaTeX itself -- `BasicText`
     * refuses an empty stand-in outright. The builder drops the mark when this returns null.
     *
     * @param formula The formula the mark carries.
     * @param placeholderPos Where in the built text the formula begins, which is what its id is
     *   keyed by: two formulas can hold the same LaTeX, and one inline content keyed to that alone
     *   would be found for both.
     */
    fun prepareInlineMeta(
        formula: Formula,
        placeholderPos: Int
    ): InlineFormulaMeta? {
        if (formula.content.isEmpty()) return null

        val inlineId = "f_${placeholderPos}_${formula.content.hashCode()}"

        return InlineFormulaMeta(
            formula = formula,
            inlineId = inlineId,
        )
    }
}
