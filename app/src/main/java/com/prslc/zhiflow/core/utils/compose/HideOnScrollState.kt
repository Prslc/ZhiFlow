package com.prslc.zhiflow.core.utils.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/** How far a gesture has to move before a bar answers its direction, in pixels. */
private const val DIRECTION_THRESHOLD = 5f

/**
 * Whether a bar pinned to a list should still be shown.
 *
 * [visible] follows the gesture: the bar goes while the list is being read downwards and comes
 * back the moment the reader turns around, which is the one direction in which they are looking
 * for something rather than at something.
 *
 * [connection] is what it listens through, and belongs on whatever holds the scrolling. It never
 * consumes, so placing it below the view that owns the scroll leaves that view's own handling —
 * pull-to-refresh, chiefly — untouched.
 *
 * The threshold is what keeps a fling from flickering it: without one, a scroll that wobbles by a
 * pixel either way would stutter the bar in and out.
 */
internal class HideOnScrollState {

    var visible by mutableStateOf(true)
        internal set

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            when {
                available.y < -DIRECTION_THRESHOLD -> visible = false
                available.y > DIRECTION_THRESHOLD -> visible = true
            }
            return Offset.Zero
        }
    }
}

@Composable
internal fun rememberHideOnScroll(): HideOnScrollState = remember { HideOnScrollState() }
