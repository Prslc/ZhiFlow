package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.LatexAutoWrap
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.prslc.zhiflow.core.utils.cleanLatex
import com.prslc.zhiflow.data.model.Formula
import com.prslc.zhiflow.parser.model.RichTextElement
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import kotlinx.serialization.json.Json

@Composable
fun LatexComponent(
    formula: Formula,
    modifier: Modifier = Modifier,
    isInline: Boolean = false,
) {
    val isDark = isSystemInDarkTheme()
    val config = remember(isDark) {
        LatexConfig(
            color = if (isDark) Color.White else Color.Black,
            darkColor = Color.White
        )
    }

    if (isInline) {
        LatexAutoWrap(
            latex = formula.content.cleanLatex(),
            modifier = modifier,
            config = config
        )
    } else {
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            LatexAutoWrap(
                latex = formula.content.cleanLatex(),
                config = config
            )
        }
    }
}

@Composable
fun FormulaTextSection(
    element: RichTextElement.ParsedText,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()

    val config = remember(isDark) {
        LatexConfig(
            color = if (isDark) Color.White else Color.Black,
            darkColor = Color.White
        )
    }
    val measurer = rememberLatexMeasurer(config)

    var measuredPlaceholders by remember(element.inlineMetas) {
        mutableStateOf<Map<String, Placeholder>>(emptyMap())
    }

    LaunchedEffect(element.inlineMetas, config) {
        val results = element.inlineMetas.associate { meta ->
            val dims = measurer.measure(meta.formula.content.cleanLatex(), config)
            val placeholder = with(density) {
                Placeholder(
                    width = dims?.widthPx?.toSp() ?: 20.sp,
                    height = dims?.heightPx?.toSp() ?: 20.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            }
            meta.inlineId to placeholder
        }
        measuredPlaceholders = results
    }

    val inlineContentMap = remember(measuredPlaceholders) {
        element.inlineMetas.associate { meta ->
            val placeholder = measuredPlaceholders[meta.inlineId] ?: Placeholder(
                width = 2.em,
                height = 1.2.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )

            meta.inlineId to InlineTextContent(placeholder) {
                LatexComponent(
                    formula = meta.formula,
                    isInline = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val dynamicLineHeight = remember(measuredPlaceholders) {
        val maxH = measuredPlaceholders.values.maxOfOrNull { it.height.value } ?: 0f
        if (maxH > 26f) (maxH + 2f).sp else 26.sp
    }

    key(measuredPlaceholders.size) {
        Text(
            text = element.content,
            modifier = modifier.pointerInput(element.content) {
                detectTapGestures { pos ->
                    layoutResult.value?.let { layout ->
                        val offset = layout.getOffsetForPosition(pos)
                        element.content.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { navigator.handleUrl(it.item) }

                        element.content.getStringAnnotations("INLINE_FORMULA_DATA", offset, offset)
                            .firstOrNull()?.let { annotation ->
                                runCatching { Json.decodeFromString<Formula>(annotation.item) }
                                    .getOrNull()?.imgUrl?.let { onImageClick(it) }
                            }
                    }
                }
            },
            inlineContent = inlineContentMap,
            onTextLayout = { layoutResult.value = it },
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = dynamicLineHeight,
                letterSpacing = 0.25.sp
            )
        )
    }
}