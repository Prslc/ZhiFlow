package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder

private const val ICON_SIZE_DP = 12f
private val ICON_LEADING_DP = 2.dp

/**
 * Builds the inline content for the speech bubbles of [this] text.
 *
 * The ranges are read back from the annotations rather than passed alongside the element, so a
 * string rebuilt for interception (`ZRichText`) still carries them, and no element signature has to
 * grow a field for them.
 *
 * @param onClick called with the range's key, or null where the paragraph that owns the range is
 *   not the one handling taps (`seg_like` only ever arrives on paragraphs).
 */
@Composable
fun AnnotatedString.rememberSegmentLikeIcons(
    tint: Color,
    onClick: ((String) -> Unit)? = null,
): Map<String, InlineTextContent> {
    val density = LocalDensity.current
    val annotations = remember(this) {
        getStringAnnotations(
            AnnotatedStringBuilder.SEGMENT_LIKE_ICON_TAG,
            0,
            length,
        )
    }

    return remember(annotations, density, tint, onClick) {
        // Sized in px through sp like the formulas: the bubble marks the text, it does not grow with it.
        val side = with(density) { ICON_SIZE_DP.dp.toPx().toSp() }

        annotations.associate { annotation ->
            AnnotatedStringBuilder.segmentLikeIconId(annotation.start) to InlineTextContent(
                placeholder = Placeholder(
                    width = side,
                    height = side,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
                children = {
                    val iconModifier = Modifier
                        .fillMaxSize()
                        .padding(start = ICON_LEADING_DP)
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = tint,
                        modifier = if (onClick != null) {
                            iconModifier.clickable { onClick(annotation.item) }
                        } else {
                            iconModifier
                        },
                    )
                },
            )
        }
    }
}

/**
 * Draws the underline a `seg_like` range carries.
 *
 * [androidx.compose.ui.text.style.TextDecoration] only knows a solid line, while this one has to
 * change style with the reader's like — so the line is drawn from the layout instead of baked into
 * a span style, and toggling the like never has to re-parse the paragraph. The two styles are
 * measured off the official client: the solid line is the thicker of the pair, the dashed one is
 * short dashes with slightly longer gaps.
 *
 * The line follows each line's baseline rather than the glyph boxes: those dip under descenders,
 * which would make the underline wobble.
 */
fun DrawScope.drawSegmentUnderline(
    layout: TextLayoutResult,
    start: Int,
    end: Int,
    color: Color,
    dashed: Boolean,
    baselineGap: Float,
) {
    val textLength = layout.layoutInput.text.length
    val from = start.coerceIn(0, textLength)
    val to = end.coerceIn(from, textLength)
    if (from >= to) return

    val thickness = if (dashed) 1.dp.toPx() else 1.5.dp.toPx()
    val pathEffect = if (dashed) {
        PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
    } else {
        null
    }

    var line = -1
    var left = 0f
    var right = 0f

    fun flush() {
        if (line < 0) return
        val y = layout.getLineBaseline(line) + baselineGap
        drawLine(
            color = color,
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = thickness,
            pathEffect = pathEffect,
        )
    }

    for (offset in from until to) {
        val box = layout.getBoundingBox(offset)
        val offsetLine = layout.getLineForOffset(offset)

        if (offsetLine != line) {
            flush()
            line = offsetLine
            left = box.left
            right = box.right
        } else {
            if (box.left < left) left = box.left
            if (box.right > right) right = box.right
        }
    }

    flush()
}
