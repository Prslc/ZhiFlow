package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.ui.component.richtext.component.LatexComponent
import com.prslc.zhiflow.ui.component.richtext.component.constrainedSize
import com.prslc.zhiflow.ui.component.richtext.component.formulaPlaceholder
import com.prslc.zhiflow.ui.component.richtext.component.rememberFormulaMaxWidth
import com.prslc.zhiflow.ui.component.richtext.component.rememberSegmentLikeIcons
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Builds [InlineTextContent] entries for inline formulas.
 *
 * The Zhihu API provides each formula's rendered image URL plus its display size in dp.
 * Placeholder bounds use those exact dp dimensions, so the layout is tight with no extra
 * vertical whitespace.
 */
@Composable
fun List<InlineFormulaMeta>.rememberInlineContent(): Map<String, InlineTextContent> {
    val density = LocalDensity.current
    val maxWidthDp = rememberFormulaMaxWidth()
    return remember(this, density, maxWidthDp) {
        this@rememberInlineContent.associate { meta ->
            val formula = meta.formula
            val (widthDp, heightDp) = constrainedSize(
                formula.width.toFloat(), formula.height.toFloat(), maxWidthDp
            )

            meta.inlineId to InlineTextContent(formulaPlaceholder(density, widthDp, heightDp)) {
                LatexComponent(
                    formula = formula,
                    isInline = true,
                    modifier = Modifier.fillMaxSize(),
                    maxWidthDp = maxWidthDp,
                )
            }
        }
    }
}

/**
 * Rich text display engine with inline formula support.
 *
 * - Extends [Text] with interceptors for `URL` and `INLINE_FORMULA_DATA` spatial gestures.
 * - Inline formulas are rendered via [LatexComponent] using the API-provided image.
 *
 * @param content The text to draw: its marks already applied, and the annotations that carry its
 *   links, formulas and `seg_like` ranges still on it.
 * @param style The text style to draw it in.
 * @param modifier Applied to the `Text`.
 * @param inlineMetas The paragraph's inline formulas. Each is drawn as an image over the
 *   placeholder the builder left in the text for it.
 * @param maxLines The most lines to draw before [overflow] applies.
 * @param overflow What to do with text that does not fit.
 */
@Composable
fun ZRichText(
    content: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    inlineMetas: List<InlineFormulaMeta> = emptyList(),
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val navigator = LocalNavigator.current
    val isDark = isSystemInDarkTheme()

    val inlineContent = inlineMetas.rememberInlineContent()

    val interceptedContent = remember(content, navigator, isDark) {
        buildAnnotatedString {
            append(content.text)

            content.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            content.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }

            // Before the loop below: it replaces the URL ranges with link annotations, which takes the
            // tag this reads them by with it.
            applyThemeSpans(content, isDark)

            content.getStringAnnotations(0, content.length).forEach { annotation ->
                if (annotation.tag == AnnotatedStringBuilder.URL_TAG) {
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = annotation.item,
                            styles = null,
                            linkInteractionListener = { clickable ->
                                val clickedUrl = (clickable as LinkAnnotation.Clickable).tag
                                navigator.handleUrl(clickedUrl)
                            }
                        ),
                        start = annotation.start,
                        end = annotation.end
                    )
                } else {
                    addStringAnnotation(
                        annotation.tag,
                        annotation.item,
                        annotation.start,
                        annotation.end
                    )
                }
            }
        }
    }

    Text(
        text = interceptedContent,
        style = style,
        inlineContent = inlineContent + interceptedContent.rememberSegmentLikeIcons(
            tint = MaterialTheme.colorScheme.outline,
        ),
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
}

