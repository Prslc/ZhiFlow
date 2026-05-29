package com.prslc.zhiflow.core.utils

import androidx.compose.ui.text.TextLayoutResult

/**
 * Returns `true` if the text content overflows the available layout
 * bounds, either via visual clipping or line ellipsis.
 */
internal fun TextLayoutResult?.isOverflowed(): Boolean {
    if (this == null) return false
    return hasVisualOverflow || (lineCount > 0 && isLineEllipsized(lineCount - 1))
}
