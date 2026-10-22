package com.prslc.zhiflow.ui.component.richtext

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

/**
 * How a content page sets its prose beyond the style each piece is drawn in: the room between
 * characters, and the multiple of the style's own line height.
 *
 * A composition local rather than a parameter: a paragraph, a quote and a list item each draw
 * themselves deep inside the body, and one place hands the reader's settings to all three.
 *
 * @param letterSpacing The room to add between characters, in sp.
 * @param lineHeight The multiple of a style's own line height.
 */
@Immutable
data class ReadingText(
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1f,
) {

    /**
     * @return [style] with the reader's spacing applied: the room added to the spacing it already
     *   asks for, and its own line height scaled.
     *
     * The room is added in sp, which is the unit the styles read here state their spacing in, and
     * which grows with the text size the page is drawn at.
     */
    fun applyTo(style: TextStyle): TextStyle = style.copy(
        letterSpacing = if (style.letterSpacing.isUnspecified) {
            style.letterSpacing
        } else {
            (style.letterSpacing.value + letterSpacing).sp
        },
        lineHeight = style.lineHeight * lineHeight,
    )
}

/** The reading settings in force where prose is drawn; the defaults change nothing. */
@SuppressLint("ComposeCompositionLocalUsage")
val LocalReadingText = compositionLocalOf { ReadingText() }
