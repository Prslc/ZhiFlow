package com.prslc.zhiflow.ui.page.people

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@Stable
class PeopleTabBarState(
    private val onOffsetChanged: (Float) -> Unit,
    private val getOffset: () -> Float,
) {
    var headerHeightPx by mutableFloatStateOf(0f)
    var totalTopHeightPx by mutableFloatStateOf(0f)

    val maxUpwardScrollPx: Float
        get() = (headerHeightPx - totalTopHeightPx).coerceAtLeast(0f)

    /**
     * How far into the collapse the bar is, from 0 to 1.
     *
     * A new value on every frame of the collapse. Read it while drawing, or through one of the
     * derived states below; read in composition it subscribes the reader to the frame-by-frame
     * value, and the whole page under it recomposes for every one of them.
     */
    val topBarAlpha: Float
        get() {
            val max = maxUpwardScrollPx
            return if (max > 0f) (-getOffset() / max).coerceIn(0f, 1f) else 0f
        }

    /**
     * The points on that fade at which the bar is drawn differently.
     *
     * Derived so that reading one is a dependency on the boolean rather than on [topBarAlpha]: a
     * collapse crosses each of these once, where it passes through every value in between.
     */
    val tabsPinned by derivedStateOf { maxUpwardScrollPx > 0f && getOffset() <= -maxUpwardScrollPx }

    /** Whether the bar has faded in far enough to be the theme's own surface. */
    val isTopBarOpaque by derivedStateOf { topBarAlpha > 0.9f }

    /** Whether the bar carries the user's name yet. */
    val isTitleShown by derivedStateOf { topBarAlpha > 0.8f }

    /** Whether the bar's own icons are drawn in the theme's colour rather than white. */
    val iconsOnSurface by derivedStateOf { topBarAlpha > 0.5f }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            val currentOffset = getOffset()
            val maxScroll = maxUpwardScrollPx

            if (maxScroll <= 0f && currentOffset == 0f) return Offset.Zero

            if (delta < 0) {
                val currentMax = if (maxScroll <= 0f) 2000f else maxScroll
                val newOffset = (currentOffset + delta).coerceIn(-currentMax, 0f)
                onOffsetChanged(newOffset)
                return Offset(0f, newOffset - currentOffset)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val delta = available.y
            val currentOffset = getOffset()
            val maxScroll = maxUpwardScrollPx

            if (maxScroll <= 0f && currentOffset == 0f) return Offset.Zero

            if (delta > 0) {
                val currentMax = if (maxScroll <= 0f) 2000f else maxScroll
                val newOffset = (currentOffset + delta).coerceIn(-currentMax, 0f)
                onOffsetChanged(newOffset)
                return Offset(0f, newOffset - currentOffset)
            }
            return Offset.Zero
        }
    }
}
