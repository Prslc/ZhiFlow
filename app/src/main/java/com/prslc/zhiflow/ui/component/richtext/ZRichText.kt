package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.ui.component.richtext.component.LatexComponent
import com.prslc.zhiflow.ui.component.richtext.component.constrainedSize
import com.prslc.zhiflow.ui.component.richtext.component.drawSegmentUnderline
import com.prslc.zhiflow.ui.component.richtext.component.formulaPlaceholder
import com.prslc.zhiflow.ui.component.richtext.component.rememberFormulaMaxWidth
import com.prslc.zhiflow.ui.component.richtext.component.rememberSegmentLikeIcons
import com.prslc.zhiflow.ui.component.richtext.component.withFormulaLineHeight
import com.prslc.zhiflow.ui.navigation.LocalNavigator

/**
 * Builds [InlineTextContent] entries for inline formulas.
 *
 * The Zhihu API provides each formula's rendered image URL plus its display size in dp.
 * Placeholder bounds use those exact dp dimensions, so the layout is tight with no extra
 * vertical whitespace.
 *
 * @param maxWidthDp The width the formulas are clamped to, or any value at or below 0 to measure
 *   the screen instead.
 */
@Composable
fun List<InlineFormulaMeta>.rememberInlineContent(
    maxWidthDp: Float = -1f
): Map<String, InlineTextContent> {
    val density = LocalDensity.current
    val effectiveMaxWidth = if (maxWidthDp > 0f) maxWidthDp else rememberFormulaMaxWidth()
    return remember(this, density, effectiveMaxWidth) {
        this@rememberInlineContent.associate { meta ->
            val formula = meta.formula
            val (widthDp, heightDp) = constrainedSize(
                formula.width.toFloat(), formula.height.toFloat(), effectiveMaxWidth
            )

            meta.inlineId to InlineTextContent(formulaPlaceholder(density, widthDp, heightDp)) {
                LatexComponent(
                    formula = formula,
                    isInline = true,
                    modifier = Modifier.fillMaxSize(),
                    maxWidthDp = effectiveMaxWidth,
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
 * @param style The text style to draw it in. Its line height is raised where an inline formula
 *   needs the room, by [withFormulaLineHeight].
 * @param modifier Applied to the `Text`.
 * @param inlineMetas The paragraph's inline formulas. Each is drawn as an image over the
 *   placeholder the builder left in the text for it.
 * @param maxFormulaWidthDp The width the formulas are clamped to, or any value at or below 0 to
 *   measure the screen instead. A host narrower than the page -- a table cell -- passes its own
 *   width.
 * @param maxLines The most lines to draw before [overflow] applies.
 * @param overflow What to do with text that does not fit.
 * @param segmentLikes The live like state of the text's `seg_like` ranges, keyed by the range's own
 *   key. Only the liked flag is read here: it picks the underline's style, and the count lives in
 *   the panel the range opens. Empty for a host that carries no ranges.
 * @param onSegmentLikeClick Called with a range's key when the reader taps the range or its bubble.
 */
@Composable
fun ZRichText(
    content: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    inlineMetas: List<InlineFormulaMeta> = emptyList(),
    maxFormulaWidthDp: Float = -1f,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    segmentLikes: Map<String, SegmentLikeTarget> = emptyMap(),
    onSegmentLikeClick: (String) -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val isDark = isSystemInDarkTheme()
    val underlineColor = MaterialTheme.colorScheme.outline

    val inlineContent = inlineMetas.rememberInlineContent(maxFormulaWidthDp)

    val interceptedContent = remember(content, navigator, isDark) {
        buildAnnotatedString {
            append(content.text)

            content.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            content.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }

            // Before the loop below: it replaces the URL ranges with link annotations, which takes
            // the tag this reads them by with it.
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

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val segmentRanges = remember(interceptedContent) {
        interceptedContent.getStringAnnotations(
            AnnotatedStringBuilder.SEGMENT_LIKE_TAG,
            0,
            interceptedContent.length,
        )
    }

    // No range, no gesture: a tap filter would swallow the click of the row the outline draws this
    // in, and a heading, a list item or a footnote carries no range to open.
    val segmentGesture = if (segmentRanges.isEmpty()) {
        Modifier
    } else {
        Modifier.pointerInput(interceptedContent) {
            detectTapGestures { pos ->
                layoutResult.value?.let { layout ->
                    val offset = layout.getOffsetForPosition(pos)
                    interceptedContent.getStringAnnotations(
                        AnnotatedStringBuilder.SEGMENT_LIKE_TAG, offset, offset
                    ).firstOrNull()?.let { onSegmentLikeClick(it.item) }
                }
            }
        }
    }

    Text(
        text = interceptedContent,
        style = style.withFormulaLineHeight(inlineMetas, maxFormulaWidthDp),
        inlineContent = inlineContent + interceptedContent.rememberSegmentLikeIcons(
            tint = underlineColor,
            onClick = onSegmentLikeClick,
        ),
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { layoutResult.value = it },
        modifier = modifier
            .drawBehind {
                val layout = layoutResult.value ?: return@drawBehind
                segmentRanges.forEach { range ->
                    drawSegmentUnderline(
                        layout = layout,
                        start = range.start,
                        end = range.end,
                        color = underlineColor,
                        dashed = segmentLikes[range.item]?.isLiked != true,
                    )
                }
            }
            .then(segmentGesture),
    )
}

