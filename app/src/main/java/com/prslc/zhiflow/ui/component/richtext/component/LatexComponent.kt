package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.data.model.content.Formula
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import kotlinx.serialization.json.Json

/**
 * Builds an inline formula placeholder sized by the server-provided dp dimensions.
 *
 * The Zhihu API reports each formula's display size in dp ([Formula.width]/[Formula.height],
 * the rendered bitmap is 3x that). The span bounds take those exact dp values as they come,
 * so formulas keep their natural size variation (simple subscripts ~13dp, display fractions up
 * to ~56dp). [Placeholder] only accepts sp/em units, so the dp target is
 * converted via density and then divided by fontScale so the final rendered pixels stay
 * constant regardless of the user's system font size.
 *
 * @param density The density the server's dp is converted through, on the way to the sp a
 *   [Placeholder] takes.
 * @param widthDp The server-reported width, in dp.
 * @param heightDp The server-reported height, in dp.
 */
fun formulaPlaceholder(density: Density, widthDp: Float, heightDp: Float): Placeholder {
    val widthSp = with(density) { widthDp.dp.toPx().toSp() }
    val heightSp = with(density) { heightDp.dp.toPx().toSp() }
    return Placeholder(
        width = widthSp,
        height = heightSp,
        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
    )
}

/**
 * Returns the max inline formula width in dp: screen width minus 42dp, so an over-wide formula
 * scales down instead of being cropped or scrolled.
 */
@Composable
@ReadOnlyComposable
internal fun rememberFormulaMaxWidth(): Float {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return (screenWidth - 2 * 21).coerceAtLeast(200).toFloat()
}

/**
 * Constrains a formula's dimensions to the content column: if the formula's width exceeds
 * [maxWidthDp], scale both dimensions proportionally so it shrinks rather than being cropped or
 * scrolled.
 */
internal fun constrainedSize(
    widthDp: Float,
    heightDp: Float,
    maxWidthDp: Float,
): Pair<Float, Float> {
    if (widthDp <= 0f || heightDp <= 0f) return widthDp to heightDp
    if (widthDp <= maxWidthDp) return widthDp to heightDp
    val scale = maxWidthDp / widthDp
    return maxWidthDp to (heightDp * scale)
}

/**
 * A formula, drawn from the image the API pre-rendered for it.
 *
 * The API never omits that image or its dp size, so nothing here measures a formula or lays one
 * out: the bitmap is loaded and drawn at the size it came with, which is what keeps a page of them
 * off the main thread. Over-wide formulas are scaled down to fit rather than cropped or scrolled.
 *
 * @param formula The formula to draw: its image url and the dp size to draw it at.
 * @param modifier Applied to the image, before the size it draws at is set.
 * @param isInline True for a formula inside a line of text, false for one on a line of its own. The
 *   two differ in padding as well as in how they are sized.
 * @param maxWidthDp The width to clamp an over-wide formula to, or any value at or below 0 to
 *   measure the screen instead.
 */
@Composable
fun LatexComponent(
    formula: Formula,
    modifier: Modifier = Modifier,
    isInline: Boolean = false,
    maxWidthDp: Float = -1f,
) {
    val effectiveMaxWidth = if (maxWidthDp > 0f) maxWidthDp else rememberFormulaMaxWidth()
    if (isInline) {
        val (widthDp, heightDp) = constrainedSize(
            formula.width.toFloat(), formula.height.toFloat(), effectiveMaxWidth
        )
        FormulaImage(
            formula = formula,
            modifier = modifier
                .width(widthDp.dp)
                .height(heightDp.dp),
        )
    } else {
        val (widthDp, heightDp) = constrainedSize(
            formula.width.toFloat(), formula.height.toFloat(), effectiveMaxWidth
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
        ) {
            FormulaImage(
                formula = formula,
                modifier = Modifier
                    .width(widthDp.dp)
                    .height(heightDp.dp),
            )
        }
    }
}

@Composable
private fun FormulaImage(
    formula: Formula,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = formula.imgUrl,
        contentDescription = formula.content,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
        colorFilter = if (isSystemInDarkTheme()) {
            // Invert the white-background formula bitmap so the background turns dark and the
            // black glyphs turn light in dark mode.
            val matrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
            ColorFilter.colorMatrix(matrix)
        } else {
            null
        }
    )
}

/**
 * A paragraph of text with inline formulas. Compose does not grow a row to fit a tall inline
 * placeholder, so the line height is raised to cover the tallest formula in it — otherwise a
 * `\displaystyle` fraction overlaps the rows around it.
 *
 * @param element The paragraph to draw. Its inline formulas are already placeholders in the text.
 * @param modifier Applied to the `Text`, ahead of the pointer input that handles taps on it.
 * @param segmentLikes The live state of the paragraph's `seg_like` ranges, keyed by the range's
 *   own key. Only the liked flag is read here: the underline style follows it, and the count lives
 *   in the panel the range opens.
 * @param onSegmentLikeClick Called with a range's key when the bubble or its underlined text is
 *   tapped.
 * @param onFormulaClick Called with a formula's image url when the reader taps it.
 */
@Composable
fun FormulaTextSection(
    element: RichTextElement.ParsedText,
    modifier: Modifier = Modifier,
    segmentLikes: Map<String, SegmentLikeTarget> = emptyMap(),
    onSegmentLikeClick: (String) -> Unit = {},
    onFormulaClick: (String) -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val density = LocalDensity.current
    val maxWidthDp = rememberFormulaMaxWidth()
    val underlineColor = MaterialTheme.colorScheme.outline

    val inlineContentMap = remember(element.inlineMetas, density, maxWidthDp) {
        element.inlineMetas.associate { meta ->
            val (widthDp, heightDp) = constrainedSize(
                meta.formula.width.toFloat(), meta.formula.height.toFloat(), maxWidthDp
            )
            meta.inlineId to InlineTextContent(formulaPlaceholder(density, widthDp, heightDp)) {
                LatexComponent(
                    formula = meta.formula,
                    isInline = true,
                    modifier = Modifier.fillMaxSize(),
                    maxWidthDp = maxWidthDp,
                )
            }
        }
    }
    val bubbleContentMap = element.content.rememberSegmentLikeIcons(
        tint = underlineColor,
        onClick = onSegmentLikeClick,
    )

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val segmentRanges = remember(element.content) {
        element.content.getStringAnnotations(
            AnnotatedStringBuilder.SEGMENT_LIKE_TAG,
            0,
            element.content.length,
        )
    }

    val maxFormulaHeightDp = element.inlineMetas.maxOfOrNull {
        constrainedSize(it.formula.width.toFloat(), it.formula.height.toFloat(), maxWidthDp).second
    } ?: 0f
    val baseLineHeight = MaterialTheme.typography.bodyLarge.lineHeight
    val effectiveLineHeight = if (maxFormulaHeightDp > 0f) {
        val formulaSp = with(density) { (maxFormulaHeightDp + 8f).dp.toSp() }
        if (formulaSp.value > baseLineHeight.value) formulaSp else baseLineHeight
    } else {
        baseLineHeight
    }

    Text(
        text = element.content,
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
                        baselineGap = 3.dp.toPx(),
                    )
                }
            }
            .pointerInput(element.content) {
                detectTapGestures { pos ->
                    layoutResult.value?.let { layout ->
                        val offset = layout.getOffsetForPosition(pos)
                        element.content.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { navigator.handleUrl(it.item) }

                        element.content.getStringAnnotations("INLINE_FORMULA_DATA", offset, offset)
                            .firstOrNull()?.let { annotation ->
                                runCatching { Json.decodeFromString<Formula>(annotation.item) }
                                    .getOrNull()?.imgUrl?.let { onFormulaClick(it) }
                            }

                        // The bubble carries its own tap target; this covers a tap that lands on
                        // the underlined text instead.
                        element.content
                            .getStringAnnotations(
                                AnnotatedStringBuilder.SEGMENT_LIKE_TAG, offset, offset
                            )
                            .firstOrNull()?.let { onSegmentLikeClick(it.item) }
                    }
                }
            },
        inlineContent = inlineContentMap + bubbleContentMap,
        onTextLayout = { layoutResult.value = it },
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = effectiveLineHeight,
            letterSpacing = 0.25.sp,
        )
    )
}
