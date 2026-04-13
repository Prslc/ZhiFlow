package com.prslc.zhiflow.parser.engine

import androidx.compose.ui.unit.Density
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.model.LatexConfig
import com.prslc.zhiflow.data.model.Formula
import com.prslc.zhiflow.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.utils.cleanLatex

object FormulaHandler {
    fun prepareInlineMeta(
        formula: Formula,
        placeholderPos: Int,
        measurer: LatexMeasurerState,
        density: Density,
        config: LatexConfig,
        isDark: Boolean
    ): InlineFormulaMeta? {
        val cleaned = formula.content.cleanLatex()
        val dims = measurer.measure(cleaned, config, isDark) ?: return null

        val inlineId = "f_${placeholderPos}_${formula.content.hashCode()}"
        return InlineFormulaMeta(
            formula = formula,
            inlineId = inlineId,
            width = with(density) { dims.widthPx.toSp() },
            height = with(density) { dims.heightPx.toSp() }
        )
    }
}