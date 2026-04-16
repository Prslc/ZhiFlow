package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.InlineTextContent
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.prslc.zhiflow.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.ui.navigation.LocalNavigator

/**
 * Asynchronously measures inline formulas and converts them into a [Map] of [InlineTextContent].
 *
 * This implementation achieves:
 * 1. **Decoupling**: The parser layer provides metadata only, while the UI layer handles
 * measurement based on the current [LocalDensity].
 * 2. **Performance**: Measurement is offloaded to a [LaunchedEffect] to avoid blocking
 * the main thread during initial composition.
 * 3. **Reactivity**: Automatically updates [measuredPlaceholders] once dimensions are
 * available, triggering a recomposition with correct physical sizes.
 */
@Composable
fun List<InlineFormulaMeta>.rememberMeasuredInlineContent(
    config: LatexConfig
): Map<String, InlineTextContent> {
    val density = LocalDensity.current
    val measurer = rememberLatexMeasurer(config)

    var measuredPlaceholders by remember(this) {
        mutableStateOf<Map<String, Placeholder>>(emptyMap())
    }

    LaunchedEffect(this, config) {
        val results = this@rememberMeasuredInlineContent.associate { meta ->
            val dims = measurer.measure(meta.formula.content, config)
            meta.inlineId to with(density) {
                Placeholder(
                    width = dims?.widthPx?.toSp() ?: 2.em,
                    height = dims?.heightPx?.toSp() ?: 1.2.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            }
        }
        measuredPlaceholders = results
    }

    return remember(measuredPlaceholders) {
        this@rememberMeasuredInlineContent.associate { meta ->
            val placeholder = measuredPlaceholders[meta.inlineId] ?: Placeholder(
                width = 2.em,
                height = 1.2.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )

            meta.inlineId to InlineTextContent(placeholder) {
                LatexComponent(
                    formula = meta.formula,
                    isInline = true
                )
            }
        }
    }
}

/**
 * A unified rich text rendering component for ZhiFlow.
 *
 * Enhancements over standard [androidx.compose.material3.Text]:
 * 1. **Async Inline Formulas**: Uses [rememberMeasuredInlineContent] to resolve formula
 * dimensions on-demand, breaking the circular dependency between parsing and UI density.
 * 2. **Precise Layout Control**: Utilizes [key] to wrap the [Text] component. This ensures
 * that once async measurements complete, the [Text] layout is invalidated and recalculated
 * to prevent overlapping or squeezed inline assets.
 * 3. **Interactive Annotations**: Built-in tap detection for "URL" and "FORMULA" tags,
 * delegating navigation to [LocalNavigator].
 * 4. **Theme Awareness**: Synchronizes LaTeX rendering colors with the system theme
 * and the provided [style].
 */
@Composable
fun ZRichText(
    modifier: Modifier = Modifier,
    content: AnnotatedString,
    inlineMetas: List<InlineFormulaMeta> = emptyList(),
    style: TextStyle,
) {
    val navigator = LocalNavigator.current
    val isDark = isSystemInDarkTheme()

    val config = remember(style.color, isDark) {
        LatexConfig(
            color = if (style.color != Color.Unspecified) style.color else (if (isDark) Color.White else Color.Black),
            darkColor = Color.White
        )
    }

    val inlineContent = inlineMetas.rememberMeasuredInlineContent(config)
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    // CRITICAL: Changing the key when the map size grows (from 0 to N) forces
    // the Text component to be fully recreated. This is necessary because
    // Compose's Text engine often fails to update inline placeholder positions
    // dynamically when dimensions change post-initial-layout.
    key(inlineContent.size) {
        Text(
            text = content,
            style = style,
            inlineContent = inlineContent,
            onTextLayout = { layoutResult.value = it },
            modifier = modifier.pointerInput(content) {
                detectTapGestures { pos ->
                    layoutResult.value?.let { layout ->
                        val offset = layout.getOffsetForPosition(pos)
                        content.getStringAnnotations(start = offset, end = offset)
                            .firstOrNull { it.tag == "URL" || it.tag == "FORMULA" }
                            ?.let { annotation ->
                                navigator.handleUrl(annotation.item)
                            }
                    }
                }
            }
        )
    }
}