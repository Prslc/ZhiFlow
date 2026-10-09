package com.prslc.zhiflow.ui.component.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

/**
 * The hairline a content page shows at its very top, tracking how far the reader has got.
 *
 * @param progress Read while drawing rather than while composing, once per frame. A caller passes
 *   a lambda that reads its own state, never a value: as a `Float` parameter it would be read in
 *   the caller's scope, and the screen would recompose on every frame the list scrolls.
 * @param modifier Applied to the bar's `Box` ahead of its own 2dp height, so a caller cannot make
 *   it thicker.
 */
@Composable
fun ReadingProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .drawBehind {
                drawRect(
                    color = color,
                    size = Size(size.width * progress().coerceIn(0f, 1f), size.height),
                )
            }
    )
}
