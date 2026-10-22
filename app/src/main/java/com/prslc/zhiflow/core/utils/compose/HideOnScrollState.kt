package com.prslc.zhiflow.core.utils.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How far a scroll has to cover to take a bar all the way away.
 *
 * Bars of different heights share it, so that they come and go over the same stretch of scroll:
 * measured against their own heights instead, the shorter one would be whole while the taller
 * was still partway back. A bar's height decides how far it travels, not how much scrolling that
 * takes.
 */
val BarCollapseDistance = 56.dp

/**
 * How far a bar pinned to a list has been pushed out of the way, as the list is read.
 *
 * A gesture pushes it one way and pulls it back the other, and it is held where the gesture
 * leaves it — a reader who stops halfway through has a bar halfway gone.
 *
 * @param maxScroll How far the bar can be pushed, which is its own height. Capping there is what
 *   makes it come back from anywhere: without a cap the distance accumulates past any bar, and
 *   the reader has to scroll all of it back before the bar starts to return.
 */
internal class HideOnScrollState(private val maxScroll: Float) {

    private var scrolled by mutableFloatStateOf(0f)

    /** 0 while the bar is whole, 1 once it is gone. */
    val fraction: Float
        get() = (scrolled / maxScroll).coerceIn(0f, 1f)

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Only a finger moves the bar: a fling carries the list on its own, and a bar
            // collapsing at that speed reads as the content being shoved around.
            if (source == NestedScrollSource.UserInput) {
                scrolled = (scrolled - available.y).coerceIn(0f, maxScroll)
            }
            return Offset.Zero
        }
    }
}

/**
 * @param barHeight The bar's height, which is both how far it has to travel and how far it can.
 */
@Composable
internal fun rememberHideOnScroll(barHeight: Dp): HideOnScrollState {
    val distance = with(LocalDensity.current) { barHeight.toPx() }
    return remember(distance) { HideOnScrollState(distance) }
}
