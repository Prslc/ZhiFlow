package com.prslc.zhiflow.ui.page.comment

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * How long a pane's move takes, and the pace the pull-back settles at. Both of the pair's moves run
 * for it, which is what lets the gesture's progress map evenly onto them.
 */
private const val PANE_MS = 300

private val PANE_MOVE = tween<IntOffset>(PANE_MS, easing = FastOutSlowInEasing)

/** How dark the pane behind goes at its darkest; it clears as that pane comes forward. */
private const val PANE_DIM = 0.32f

/**
 * How often the stall watch samples the transition. Anything under the shortest drive will do: a
 * drive that is running moves the fraction between two samples, a dead one does not.
 */
private const val STALL_CHECK_MS = 150L

/**
 * The two panes of a comment surface — a root list and one root comment's replies — as a single
 * seekable transition, so the back gesture drives the exit under the finger, and the header's
 * arrow, the back key and a released gesture all land on the same specs: the app's own push and pop
 * (MainActivity). The content's sheet and a passage's panel both show this pair, and share it from
 * here rather than keeping a copy of the traps below each.
 *
 * Three traps come with that shape.
 *
 * A drive has to be re-issued rather than issued once: the framework hands its mutator mutex
 * between callers, so a gesture's seek, the next intent or a pull-back cancels whichever drive is
 * running, and a cancelled drive leaves the panes part-way across with nobody left to finish them.
 * The stall watch is what repairs that.
 *
 * The intent has to be read through `rememberUpdatedState`: [isDetail] is a parameter, a value
 * rather than a state holder, so anything outliving a recomposition would keep reading the one it
 * was composed with.
 *
 * And a commit has to be judged by where the panes *are*, not by `targetState`: a gesture's seek
 * has already pointed that at the side being asked for, so asking it whether there is anything to
 * do answers no, and the panes stay where the finger let go until the stall watch's next sample.
 *
 * @param backEnabled Whether the surface is up and staying. A back is taken only then: once one has
 *   sent the surface on its way, later ones belong to the screen behind. The root pane keeps the
 *   surface's own dismissal gesture either way.
 */
@Composable
fun CommentPaneTransition(
    isDetail: Boolean,
    backEnabled: Boolean,
    onBackToMain: () -> Unit,
    main: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detailState = remember { SeekableTransitionState(isDetail) }
    val detailTransition = rememberTransition(detailState, label = "CommentPanes")
    val scope = rememberCoroutineScope()
    val detailIntent by rememberUpdatedState(isDetail)
    var isGesturing by remember { mutableStateOf(false) }

    LaunchedEffect(isDetail) {
        // Judged by rest, not by targetState: a gesture's seek has already pointed that at this side.
        if (!detailState.isAt(isDetail)) {
            if (isDetail) detailState.animateTo(true) else detailState.completeExit()
        }
    }

    // The only thing that finishes a cancelled drive; see the note on this composable.
    LaunchedEffect(Unit) {
        var lastFraction = detailState.fraction
        while (true) {
            delay(STALL_CHECK_MS)
            val fraction = detailState.fraction
            val stalled = fraction == lastFraction &&
                    !isGesturing &&
                    !detailState.isAt(detailIntent)
            lastFraction = fraction
            if (!stalled) continue
            try {
                detailState.driveTo(detailIntent)
            } catch (e: CancellationException) {
                // Cancelled by whatever else wanted the panes; the next sample looks again.
                if (!isActive) throw e
            }
        }
    }

    // The latch the stall watch reads cannot outlive the handler: no gesture is in flight while the
    // handler is disabled, and one whose flow never ended would lock the watch out for good.
    LaunchedEffect(backEnabled, isDetail) { isGesturing = false }

    PredictiveBackHandler(enabled = backEnabled && isDetail) { progress ->
        try {
            isGesturing = true
            progress.collect { detailState.seekTo(it.progress, targetState = false) }
        } catch (e: CancellationException) {
            // This job is already dead, so the pull-back runs in a scope that outlives it.
            scope.launch { detailState.settleToRest() }
            throw e
        } finally {
            isGesturing = false
        }
        // Outside the try: a gesture pulled back out of must not commit a back.
        onBackToMain()
    }

    // The pane behind carries the dim, the way the page being covered does in MainActivity; riding the
    // transition's own play time is what keeps it on the same curve as the move.
    val behind by detailTransition.animateFloat(
        transitionSpec = { tween(PANE_MS, easing = FastOutSlowInEasing) },
        label = "paneBehind",
    ) { if (it) 1f else 0f }

    detailTransition.AnimatedContent(
        transitionSpec = {
            // The app's own push and pop (MainActivity) without their fades: the arriving pane takes
            // the full width while the one behind it drifts a fifth.
            if (targetState) {
                slideInHorizontally(PANE_MOVE) { it } togetherWith
                        slideOutHorizontally(PANE_MOVE) { -it / 5 }
            } else {
                // The arriving pane is the one that was behind, so the leaving one stays on top: at the
                // default order the parent would be drawn over the pane it is revealing.
                ContentTransform(
                    targetContentEnter = slideInHorizontally(PANE_MOVE) { -it / 5 },
                    initialContentExit = slideOutHorizontally(PANE_MOVE) { it },
                    targetContentZIndex = -1f,
                )
            }
        }
    ) { showDetail ->
        // Each pane paints the surface: the two are stacked in one box and neither fills itself, so
        // the pane behind would show through the one drawn on top of it.
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            if (showDetail) detail() else main()

            // The root pane is the one behind, in both directions: the replies cover it on the way
            // in, and on the way back it is what the replies uncover.
            if (!showDetail) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = PANE_DIM * behind)),
                )
            }
        }
    }
}

/** Whether the panes have finished moving to [detail], as opposed to being on their way there. */
private fun SeekableTransitionState<Boolean>.isAt(detail: Boolean): Boolean =
    currentState == detail && targetState == detail

/**
 * Puts the panes on [detail]: walks them over when they are on the other side, and brings them
 * back to the side they came from when a gesture left them part-way across it.
 */
private suspend fun SeekableTransitionState<Boolean>.driveTo(detail: Boolean) {
    if (currentState != detail) {
        if (detail) animateTo(true) else completeExit()
    } else {
        settleToRest()
    }
}

/**
 * Walks the detail pane out. A gesture that already carried the progress to its end leaves the
 * panes where the exit would end up, so the state is taken rather than walked: `animateTo` flips
 * `currentState` in that case too, but only after waiting for a composition, and at a fraction of 1
 * it has nothing to animate in the meantime.
 */
private suspend fun SeekableTransitionState<Boolean>.completeExit() {
    if (fraction < 1f) animateTo(false) else snapTo(false)
}

/**
 * Walks a half-way transition back to the side it started from. `seekTo` suspends, so the return is
 * stepped one frame at a time rather than driven by an animation; the closing `snapTo` puts the
 * target back on the rest state, or the next gesture would still read the other side as its
 * destination.
 */
private suspend fun SeekableTransitionState<Boolean>.settleToRest() {
    val rest = currentState
    val start = fraction
    if (start <= 0f) {
        snapTo(rest)
        return
    }
    val returnAnimation = TargetBasedAnimation(
        animationSpec = tween((PANE_MS * start).toInt(), easing = LinearOutSlowInEasing),
        typeConverter = Float.VectorConverter,
        initialValue = start,
        targetValue = 0f,
    )
    var startNanos = -1L
    while (true) {
        val value = withFrameNanos { now ->
            if (startNanos < 0L) startNanos = now
            returnAnimation.getValueFromNanos(now - startNanos)
        }
        if (value <= 0f) break
        seekTo(value, targetState = !rest)
    }
    snapTo(rest)
}
