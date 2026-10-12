package com.prslc.zhiflow.core.utils.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.prslc.zhiflow.core.utils.imageUrlAt

/**
 * The picture at the size it is about to be drawn at, for an `AsyncImage`'s model.
 *
 * A picture drawn larger than the copy the server handed over is upscaled, which is what leaves a
 * wide avatar or a full-screen image looking soft; [imageUrlAt] is what is done about it.
 *
 * @param url The url the server handed out, or null to leave the loader its placeholder.
 * @param size How wide the picture will be drawn.
 * @return The url at that size, or null where there was none to begin with.
 */
@Composable
@ReadOnlyComposable
internal fun sizedImageUrl(url: String?, size: Dp): String? =
    url?.let { imageUrlAt(it, with(LocalDensity.current) { size.roundToPx() }) }
