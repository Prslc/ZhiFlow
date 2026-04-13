package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.prslc.zhiflow.parser.InlineFormulaMeta
import com.prslc.zhiflow.ui.navigation.LocalNavigator

/**
 * Converts a list of [InlineFormulaMeta] into a [Map] of [InlineTextContent]
 * required by Compose's [Text] component for rendering inline assets.
 */
@Composable
fun List<InlineFormulaMeta>.rememberInlineContent(): Map<String, InlineTextContent> {
    return remember(this) {
        this.associate { meta ->
            meta.inlineId to InlineTextContent(
                Placeholder(meta.width, meta.height, PlaceholderVerticalAlign.Center)
            ) {
                LatexComponent(formula = meta.formula, isInline = true)
            }
        }
    }
}


/**
 * A unified rich text rendering component for ZhiFlow.
 *
 * This component acts as a high-level wrapper around [Text], specifically designed to handle:
 * 1. **Inline Formulas**: Automatically converts [InlineFormulaMeta] into [InlineTextContent]
 * using [LatexComponent].
 * 2. **Interactive Elements**: Detects tap gestures on specific annotations like "URL"
 * and "FORMULA", delegating navigation logic to [LocalNavigator].
 * 3. **Performance Optimization**: Efficiently manages [remember] blocks for inline content
 * and layout results to minimize unnecessary recompositions.
 *
 * @param content The annotated string containing text and potential link/formula annotations.
 * @param inlineMetas Metadata for inline formulas. Defaults to an empty list for plain text/links.
 * @param style The text style to be applied.
 * @param modifier Modifier for positioning or sizing this text block.
 */
@Composable
fun ZRichText(
    modifier: Modifier = Modifier,
    content: AnnotatedString,
    inlineMetas: List<InlineFormulaMeta> = emptyList(),
    style: TextStyle,
) {
    val navigator = LocalNavigator.current
    val inlineContent = inlineMetas.rememberInlineContent()
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

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