package com.prslc.zhiflow.core.utils

import androidx.compose.ui.text.TextLayoutResult

/**
 * Text overflow detection extension
 */
fun TextLayoutResult?.isOverflowed(): Boolean {
    if (this == null) return false
    return hasVisualOverflow || (lineCount > 0 && isLineEllipsized(lineCount - 1))
}