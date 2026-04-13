package com.prslc.zhiflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

object TextStyles {
    val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)

    // card
    val cardTitleBold = FontWeight.Bold
    val cardTitleLineHeight = 18.sp
    val cardDescSize = 12.sp

    // rich text
    fun linkStyle(isDark: Boolean) = SpanStyle(
        color = if (isDark) ZhihuBlueDark else ZhihuBlue,
        fontWeight = FontWeight.Medium,
        textDecoration = TextDecoration.None
    )

    fun codeStyle(isDark: Boolean) = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = if (isDark) CodeBackgroundDark else CodeBackgroundLight,
        fontSize = 14.sp
    )

    fun referenceStyle(isDark: Boolean) = SpanStyle(
        fontSize = 11.sp,
        baselineShift = BaselineShift.Superscript,
        fontWeight = FontWeight.Bold,
        color = if (isDark) ZhihuBlueDark else ZhihuBlue
    )

    fun headingStyle(level: Int): TextStyle = when (level) {
        1 -> Typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
        2 -> Typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        3 -> Typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        else -> Typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    }

    // comment
    val commentMetaSize = 12.sp
    val commentLikeSize = 11.sp
    val emojiSize = 18.sp

    // img
    val imageCaptionSize = 13.sp
}