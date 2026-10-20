package com.prslc.zhiflow.ui.page.content

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * [TopBarLeaveState] for this page.
 *
 * @param canLeave Whether the bar may start leaving yet. A bar with a collapse of its own hands in
 *   what finishes it; one that never folds is free from the first scroll.
 */
@Composable
internal fun rememberTopBarLeave(canLeave: () -> Boolean): TopBarLeaveState =
    remember { TopBarLeaveState(canLeave) }

/**
 * The top bar leaving, under the finger, once the bar has done what it does by itself.
 *
 * It goes and comes back with the scroll and takes what it uses: the layout shortens the bar by
 * what the list gave up, so the words under the finger move once -- the same bargain the collapse
 * makes a stage earlier. Only a hand moves it: a fling carries the list further than a hand could,
 * and a bar moving at that speed reads as the content being shoved around.
 *
 * Where it stops is the status bar's pad, which the page paints above it and does not shorten: the
 * bar's own row leaves, the content never goes under the system's bar.
 */
@Stable
internal class TopBarLeaveState(private val canLeave: () -> Boolean) {
    private var retracted by mutableFloatStateOf(0f)

    /** The bar's own travel, which the layout publishes: it is the only thing that measures it. */
    private var limitPx = 0f

    /** How far the bar is out, which the layout both shortens it by and shifts it up by. */
    val offsetPx: Float
        get() = if (canLeave()) retracted.coerceIn(0f, limitPx) else 0f

    fun record(heightPx: Int) {
        limitPx = heightPx.toFloat()
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // A fling may finish the collapse without this seeing a scroll it may act on, so a bar
            // still waiting for it is put back and the whole delta handed on.
            if (!canLeave()) {
                retracted = 0f
                return Offset.Zero
            }
            if (source != NestedScrollSource.UserInput) return Offset.Zero

            val before = retracted
            retracted = (before - available.y).coerceIn(0f, limitPx)
            return Offset(0f, -(retracted - before))
        }
    }
}

/**
 * The status bar's pad at the top of the bar, painted in the bar's own container color.
 *
 * It is the bar's first line and it stays behind when the bar leaves: the content never goes under
 * the system's status bar, which the app does not hide. Material moves that color between two
 * values as the bar collapses, so the pad is moved with it rather than showing a seam.
 */
@Composable
internal fun TopBarStrip(scrollBehavior: TopAppBarScrollBehavior, padPx: Int) {
    val containerColor = MaterialTheme.colorScheme.background
    val scrolledColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(LocalDensity.current) { padPx.toDp() })
            // Read while drawing: the fraction is a new value on every frame of the collapse, and
            // the pad only has to be repainted for it, not recomposed.
            .drawBehind {
                drawRect(
                    lerp(
                        containerColor,
                        scrolledColor,
                        FastOutLinearInEasing.transform(scrollBehavior.state.collapsedFraction),
                    ),
                )
            },
    )
}

/**
 * Shortens the bar by as much as [state] has it away and draws it shifted up by as much, so what
 * the finger pushed away leaves the layout with it rather than leaving a hole under it. The height
 * goes back to the state, which has no other way to know how far it may go.
 */
internal fun Modifier.retractedBy(state: TopBarLeaveState): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        state.record(placeable.height)
        val shift = state.offsetPx.coerceIn(0f, placeable.height.toFloat()).toInt()
        val visible = (placeable.height - shift).coerceIn(0, constraints.maxHeight)
        layout(placeable.width, visible) { placeable.place(0, visible - placeable.height) }
    }
