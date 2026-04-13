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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.LatexAutoWrap
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.prslc.zhiflow.data.model.Formula
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
    content: AnnotatedString,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val config = LatexConfig(
        color = if (isDark) Color.White else Color.Black,
        darkColor = Color.White
    )

    val navigator = LocalNavigator.current
    val density = LocalDensity.current
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val measurer = rememberLatexMeasurer(config)

    val inlineContentMap = remember(content, config) {
        val map = mutableMapOf<String, InlineTextContent>()
        content.getStringAnnotations("INLINE_FORMULA_DATA", 0, content.length)
            .forEach { annotation ->
                val formula = Json.decodeFromString<Formula>(annotation.item)
                val cleaned = formula.content.cleanLatex()
                val dims = measurer.measure(cleaned, config)

                if (dims != null) {
                    val hashCode = formula.content.hashCode()
                    val inlineId = "f_${annotation.start}_$hashCode"

                    map[inlineId] = InlineTextContent(
                        placeholder = Placeholder(
                            width = with(density) { dims.widthPx.toSp() },
                            height = with(density) { dims.heightPx.toSp() },
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        )
                    ) {
                        LatexComponent(
                            formula = formula,
                            isInline = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        map
    }

    val dynamicLineHeight = remember(inlineContentMap) {
        val maxH = inlineContentMap.values.maxOfOrNull { it.placeholder.height.value } ?: 0f
        if (maxH > 26f) (maxH + 2f).sp else 26.sp
    }

    Text(
        text = content,
        modifier = modifier.pointerInput(content) {
            detectTapGestures { pos ->
                layoutResult.value?.let { layout ->
                    val offset = layout.getOffsetForPosition(pos)
                    content.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { navigator.handleUrl(it.item) }

                    content.getStringAnnotations("INLINE_FORMULA_DATA", offset, offset)
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

fun String.cleanLatex(): String {
    return this
        .replace("\\,", " ")
        .replace("\\;", " ")
        .replace("\\{", "\\lbrace ")
        .replace("\\}", "\\rbrace ")
        .replace("\\mid", " | ")
        .trimEnd('\\')
}