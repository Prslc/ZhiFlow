package com.prslc.zhiflow.parser.engine

import androidx.compose.runtime.Stable
import com.prslc.zhiflow.data.model.Formula
import com.prslc.zhiflow.parser.model.InlineFormulaMeta

@Stable
object FormulaHandler {
    fun prepareInlineMeta(
        formula: Formula,
        placeholderPos: Int
    ): InlineFormulaMeta {
        val inlineId = "f_${placeholderPos}_${formula.content.hashCode()}"

        return InlineFormulaMeta(
            formula = formula,
            inlineId = inlineId
        )
    }
}