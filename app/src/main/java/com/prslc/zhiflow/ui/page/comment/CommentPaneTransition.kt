package com.prslc.zhiflow.ui.page.comment

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.IntOffset
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * How long the exit takes, and the pace the pull-back settles at. All four of the exit's animations
 * run for it, which is what lets the gesture's progress map evenly onto them.
 */
private const val DETAIL_EXIT_MS = 300

private val EXIT_MOVE = tween<IntOffset>(DETAIL_EXIT_MS, easing = FastOutSlowInEasing)
private val EXIT_FADE = tween<Float>(DETAIL_EXIT_MS, easing = FastOutSlowInEasing)

/**
 * How often the stall watch samples the transition. Anything under the shortest drive will do: a
 * drive that is running moves the fraction between two samples, a dead one does not.
 */
private const val STALL_CHECK_MS = 150L

/**
 * The two panes of a comment surface — a root list and one root comment's replies — as a single
 * seekable transition, so the back gesture drives the exit under the finger, and the header's arrow,
 * the back key and a released gesture all land on the same specs: the app's own pop language
 * (MainActivity). The content's sheet and a passage's panel both show this pair, and share it from
 * here rather than keeping a copy of the traps below each.
 *
 * Two traps come with that shape. A drive has to be re-issued rather than issued once: the framework
 * hands its mutator mutex between callers, so a gesture's seek, the next intent or a pull-back
 * cancels whichever drive is running, and a cancelled drive leaves the panes part-way across with
 * nobody left to finish them. The stall watch is what repairs that.
 *
 * And the intent has to be read through `rememberUpdatedState`: [isDetail] is a parameter, a value
 * rather than a state holder, so anything outliving a recomposition would keep reading the one it
 * was composed with.
 *
 * @param backEnabled whether the surface is up and staying. A back is taken only then: once one has
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
) {
    val detailState = remember { SeekableTransitionState(isDetail) }
    val detailTransition = rememberTransition(detailState, label = "CommentPanes")
    val scope = rememberCoroutineScope()
    val detailIntent by rememberUpdatedState(isDetail)
    var isGesturing by remember { mutableStateOf(false) }

    LaunchedEffect(isDetail) {
        if (detailState.targetState != isDetail) {
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

    detailTransition.AnimatedContent(
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            } else {
                // The incoming pane is placed on top, so the outgoing one fades as it leaves --
                // otherwise its content would show through the arriving list.
                (slideInHorizontally(EXIT_MOVE) { -it / 5 } + fadeIn(EXIT_FADE)) togetherWith
                        (slideOutHorizontally(EXIT_MOVE) { it } + fadeOut(EXIT_FADE))
            }
        }
    ) { showDetail ->
        if (showDetail) detail() else main()
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
 * Walks the detail pane out. A gesture that already carried the progress to its end leaves
 * `currentState` where it was and `animateTo` no longer flips it, so that case snaps instead.
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
        animationSpec = tween((DETAIL_EXIT_MS * start).toInt(), easing = LinearOutSlowInEasing),
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
