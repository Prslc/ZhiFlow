package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The gradient a full-bleed cover needs behind the status bar.
 *
 * Nothing drawn on a cover can take its contrast from arbitrary user art. A control can carry a
 * disc of its own -- see [iconScrim] -- but the status bar icons are drawn by the system and
 * cannot, so the strip behind them gets this instead. 0.47 black holds white icons at 3.3:1 over
 * a pure white cover.
 *
 * Place it over the cover and under everything else. It deliberately stops just past the status
 * bar: it is not a general purpose header scrim.
 */
@Composable
fun StatusBarScrim(modifier: Modifier = Modifier) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + 20.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.50f),
                    0.6f to Color.Black.copy(alpha = 0.42f),
                    1f to Color.Transparent,
                )
            )
    )
}

private val IconScrimRadius = 20.dp

/**
 * The disc an icon needs while it sits on a cover.
 *
 * 40dp across: what M3's own icon buttons draw, and what Twitter's top bar measures (40.5dp off a
 * screenshot, around a 24dp glyph). Bigger than that -- a disc the size of the whole 48dp touch
 * target -- leaves 12dp of black around the glyph and hides far more of the cover than the icon
 * needs. Smaller reads as a pinched dot. [drawBehind] rather than `background` keeps the touch
 * target at 48dp regardless.
 *
 * @param alpha Strength of the disc. 0.55 holds a white glyph at 4.7:1 even over a white cover;
 * pass less as an opaque bar fades in over it.
 */
fun Modifier.iconScrim(alpha: Float): Modifier = drawBehind {
    drawCircle(color = Color.Black.copy(alpha = alpha), radius = IconScrimRadius.toPx())
}
