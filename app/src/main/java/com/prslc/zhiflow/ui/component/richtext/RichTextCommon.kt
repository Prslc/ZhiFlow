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
 * Layout and lifecycle container for resolved inline assets.
 *
 * Provides a reactive [refreshKey] to force-reset the parent text layout
 * once the dimensions transition from placeholders to actual measured pixels.
 */
class MeasuredInlineData(
    val inlineContent: Map<String, InlineTextContent>,
    val refreshKey: Any,
)

/**
 * Measures inline formula bounds asynchronously without blocking the main thread.
 *
 * - Isolates calculation from theme/color changes by using a geometric-only [LatexConfig].
 * - Automatically scales dimensions using the ambient [LocalDensity] and provided [fontSize].
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
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
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
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            )

            meta.inlineId to InlineTextContent(placeholder) {
                LatexComponent(
                    formula = meta.formula,
                    isInline = true,
                )
            }
        }

        // Combine the metadata count and the async measurement resolution state into a single token.
        // This token reliably notifies the parent layout when the text engine needs a hard reset.
        val hasMeasured = measuredPlaceholders.isNotEmpty() || this@rememberMeasuredInlineContent.isEmpty()
        MeasuredInlineData(
            inlineContent = contentMap,
            refreshKey = "${this@rememberMeasuredInlineContent.size}_${hasMeasured}",
        )
    }
}

/**
 * Rich text display engine with native support for asynchronous inline formulas.
 *
 * - Extends [Text] with interceptors for `URL` and `FORMULA` spatial gestures.
 * - Enforces an internal `key` reset on measurement resolution to prevent inline layout overlaps.
 * - Decouples layout measurements from drawing color environments to bypass redundant recompositions.
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

    // CRITICAL: Force rebuild the Text node via `key` when async measurement finishes.
    // Native Compose Text engine fails to refresh inline asset layout boundaries on dynamic size updates.
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
