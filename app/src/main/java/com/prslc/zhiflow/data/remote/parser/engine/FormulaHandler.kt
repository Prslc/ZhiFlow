package com.prslc.zhiflow.data.remote.parser.engine

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.content.Formula
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta

@Immutable
object FormulaHandler {
    fun prepareInlineMeta(
        formula: Formula,
        placeholderPos: Int
    ): InlineFormulaMeta {
        val inlineId = "f_${placeholderPos}_${formula.content.hashCode()}"

        return InlineFormulaMeta(
            formula = formula,
            inlineId = inlineId,
        )
    }
}
