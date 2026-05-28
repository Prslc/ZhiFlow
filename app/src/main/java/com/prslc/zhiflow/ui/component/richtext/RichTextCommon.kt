package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.prslc.zhiflow.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.ui.navigation.LocalNavigator

/**
 * A wrapper class that bundles the resolved [InlineTextContent] map along with a
 * dynamic refresh key. This state package ensures that the rendering layer can
 * precisely react when asynchronous layout dimensions transition from generic
 * fallbacks to accurate physical pixels.
 */
class MeasuredInlineData(
    val inlineContent: Map<String, InlineTextContent>,
    val refreshKey: Any
)

/**
 * Asynchronously measures inline formulas and converts them into a [Map] of [InlineTextContent].
 *
 * This implementation achieves:
 * 1. **Decoupling**: The parser layer provides metadata only, while the UI layer handles
 * measurement based on the current [LocalDensity].
 * 2. **Performance**: Measurement is offloaded to a [LaunchedEffect] to avoid blocking
 * the main thread during initial composition.
 * 3. **Reactivity**: Automatically updates `measuredPlaceholders` once dimensions are
 * available, triggering a recomposition with correct physical sizes.
 * 4. **Theme Decoupling**: Utilizes a lightweight, color-agnostic [LatexConfig] solely
 * dedicated to geometric layout math, preventing unnecessary recalculations when switching themes.
 */
@Composable
fun List<InlineFormulaMeta>.rememberMeasuredInlineContent(
    fontSize: TextUnit
): MeasuredInlineData {
    val density = LocalDensity.current

    // Create a lightweight configuration dedicated strictly to structural measurements.
    // Since width/height calculations are purely geometric, we safely bypass visual themes here.
    val measureConfig = remember(fontSize) { LatexConfig(fontSize = fontSize) }
    val measurer = rememberLatexMeasurer(measureConfig)

    var measuredPlaceholders by remember(this) {
        mutableStateOf<Map<String, Placeholder>>(emptyMap())
    }

    LaunchedEffect(this, measureConfig) {
        if (this@rememberMeasuredInlineContent.isEmpty()) return@LaunchedEffect

        val results = this@rememberMeasuredInlineContent.associate { meta ->
            val dims = measurer.measure(meta.formula.content, measureConfig)
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

    return remember(this, measuredPlaceholders) {
        val contentMap = this@rememberMeasuredInlineContent.associate { meta ->
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

        // Combine the metadata count and the async measurement resolution state into a single token.
        // This token reliably notifies the parent layout when the text engine needs a hard reset.
        val hasMeasured = measuredPlaceholders.isNotEmpty() || this@rememberMeasuredInlineContent.isEmpty()
        MeasuredInlineData(
            inlineContent = contentMap,
            refreshKey = "${this@rememberMeasuredInlineContent.size}_${hasMeasured}"
        )
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
 * 4. **Theme Awareness**: Synchronizes LaTeX rendering colors automatically by relying on
 * standard adaptive components internally, maintaining separation of concerns between Text and Math.
 */
@Composable
fun ZRichText(
    content: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    inlineMetas: List<InlineFormulaMeta> = emptyList()
) {
    val navigator = LocalNavigator.current

    // Dynamically query measured layout payloads using the ambient TextStyle's font configuration
    val measuredData = inlineMetas.rememberMeasuredInlineContent(fontSize = style.fontSize)
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    // CRITICAL: Changing the key when the async measurements complete (transitioning from false to true)
    // forces the Text component to be fully recreated. This is necessary because Compose's Text engine
    // often fails to update inline placeholder positions dynamically when dimensions change post-initial-layout.
    // Relying strictly on map.size is insufficient since the count remains constant before and after layout evaluation.
    key(measuredData.refreshKey) {
        Text(
            text = content,
            style = style,
            inlineContent = measuredData.inlineContent,
            onTextLayout = { layoutResult.value = it },
            modifier = modifier.pointerInput(content) {
                detectTapGestures { pos ->
                    layoutResult.value?.let { layout ->
                        val offset = layout.getOffsetForPosition(pos)
                        val annotations = content.getStringAnnotations(start = offset, end = offset)

                        // Prioritize structural URLs over inline formulas to avoid click collision
                        val targetAnnotation = annotations.firstOrNull { it.tag == "URL" }
                            ?: annotations.firstOrNull { it.tag == "FORMULA" }

                        targetAnnotation?.let { annotation ->
                            navigator.handleUrl(annotation.item)
                        }
                    }
                }
            }
        )
    }
}