package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.ui.component.richtext.component.LatexComponent
import com.prslc.zhiflow.ui.navigation.LocalNavigator

private val InlineFormulaHeightSp = 22.sp

/**
 * Builds [InlineTextContent] entries for inline formulas.
 *
 * The Zhihu API provides each formula's rendered image URL plus its intrinsic
 * width/height, so placeholder bounds are computed directly from those values —
 * no measurement pass is required.
 */
@Composable
fun List<InlineFormulaMeta>.rememberInlineContent(): Map<String, InlineTextContent> {
    return remember(this) {
        this@rememberInlineContent.associate { meta ->
            val formula = meta.formula
            val aspectRatio = if (formula.width > 0 && formula.height > 0) {
                formula.width.toFloat() / formula.height.toFloat()
            } else {
                1f
            }
            val placeholder = Placeholder(
                width = InlineFormulaHeightSp * aspectRatio,
                height = InlineFormulaHeightSp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            )

            meta.inlineId to InlineTextContent(placeholder) {
                LatexComponent(
                    formula = formula,
                    isInline = true,
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
 */
@Composable
fun ZRichText(
    content: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    inlineMetas: List<InlineFormulaMeta> = emptyList()
) {
    val navigator = LocalNavigator.current

    val inlineContent = inlineMetas.rememberInlineContent()

    val interceptedContent = remember(content, navigator) {
        buildAnnotatedString {
            append(content.text)

            content.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            content.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }

            content.getStringAnnotations(0, content.length).forEach { annotation ->
                if (annotation.tag == "URL") {
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
        inlineContent = inlineContent,
        modifier = modifier
    )
}
