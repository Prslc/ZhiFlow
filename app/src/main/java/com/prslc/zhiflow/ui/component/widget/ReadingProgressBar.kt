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
            // Read while drawing, not while composing: a Float parameter would read the state in the
            // caller's scope and recompose the screen on every frame the list scrolls.
            .drawBehind {
                drawRect(
                    color = color,
                    size = Size(size.width * progress().coerceIn(0f, 1f), size.height),
                )
            }
    )
}
