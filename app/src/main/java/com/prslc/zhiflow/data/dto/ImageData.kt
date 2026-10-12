package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

/**
 * One image and the box it is drawn in.
 *
 * [width] and [height] carry that box's proportions, not a pixel size: a thumbnail is drawn at
 * their ratio, and a feed card's images already carry the ratio its own style asks for.
 */
@Immutable
data class ImageData(
    val url: String,
    val width: Int,
    val height: Int,
)
