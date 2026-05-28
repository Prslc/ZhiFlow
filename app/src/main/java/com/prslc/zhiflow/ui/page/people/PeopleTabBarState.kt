package com.prslc.zhiflow.ui.page.people

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@Stable
class PeopleTabBarState(
    private val onOffsetChanged: (Float) -> Unit,
    private val getOffset: () -> Float
) {
    var headerHeightPx by mutableFloatStateOf(0f)
    var totalTopHeightPx by mutableFloatStateOf(0f)

    val maxUpwardScrollPx: Float
        get() = (headerHeightPx - totalTopHeightPx).coerceAtLeast(0f)

    val topBarAlpha: Float
        get() {
            val max = maxUpwardScrollPx
            return if (max > 0f) (-getOffset() / max).coerceIn(0f, 1f) else 0f
        }

    val isTabsPinned: Boolean
        get() = maxUpwardScrollPx > 0f && getOffset() <= -maxUpwardScrollPx

    val compensatedHeaderHeight: Float
        get() = when {
            headerHeightPx > 0f -> headerHeightPx
            getOffset() != 0f -> -getOffset() + totalTopHeightPx
            else -> 0f
        }

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
            source: NestedScrollSource
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