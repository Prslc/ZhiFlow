package com.prslc.zhiflow.ui.component.widget

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

private const val MAX_HEIGHT_FRACTION = 0.95f

// The sheet sets the pace and the scrim follows it. The backdrop lands ahead of the sheet so the
// feed is already dimmed by the time the sheet settles; the exit is shorter than the entry, as a
// dismissal should be.
private const val SCRIM_FADE_IN_MS = 200
private const val ENTER_DURATION_MS = 320
private const val EXIT_DURATION_MS = 220

/**
 * A sheet a screen owns: it draws the scrim, the surface and the exits, and the caller supplies only
 * the content. The back progress it tracks is the predictive-back gesture's own, 0..1.
 */
@Composable
fun CustomBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }

    val backProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(visible) {
        transitionState.targetState = visible
        // A gesture can leave progress non-zero; the next show starts from 0.
        if (visible) backProgress.snapTo(0f)
    }

    LaunchedEffect(transitionState.currentState, transitionState.targetState) {
        if (!transitionState.targetState && !transitionState.currentState && visible) {
            onDismissRequest()
        }
    }

    // Unconditional on purpose: the handler settles precedence by composition order.
    PredictiveBackHandler(enabled = visible || transitionState.currentState) { progress ->
        try {
            progress.collect { event -> backProgress.snapTo(event.progress) }
        } catch (e: CancellationException) {
            // This job is already dead, so the spring-back runs in a scope that outlives it.
            scope.launch {
                backProgress.animateTo(
                    0f,
                    tween(EXIT_DURATION_MS, easing = LinearOutSlowInEasing),
                )
            }
            throw e
        }
        // Outside the try: a commit run in the gesture job would be cancelled by the next
        // gesture and land in the catch above, reading as a pull-back.
        scope.launch {
            // Walk the gesture track to its end first, so the ordinary exit plays out of sight
            // and the handoff cannot jump. Paced by the distance the finger actually left.
            backProgress.animateTo(
                1f,
                tween(
                    durationMillis = (EXIT_DURATION_MS * (1f - backProgress.value)).toInt(),
                    easing = FastOutLinearInEasing,
                ),
            )
            transitionState.targetState = false
        }
    }

    // The container animates nothing itself. A fade at this level would reach the sheet as well as
    // the scrim, sliding it in and out at partial opacity with the feed showing through it. Each
    // child carries its own animation instead, and the container still outlives them both.
    AnimatedVisibility(
        visibleState = transitionState,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
        modifier = modifier.zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // A host sitting inside a pager would otherwise swipe the page out from
                // under the sheet. Only horizontal drags nobody else claimed are taken.
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ -> change.consume() }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .animateEnterExit(
                        enter = fadeIn(animationSpec = tween(SCRIM_FADE_IN_MS)),
                        exit = fadeOut(animationSpec = tween(EXIT_DURATION_MS))
                    )
                    .graphicsLayer { alpha = 1f - backProgress.value }
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { transitionState.targetState = false })
                    }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * MAX_HEIGHT_FRACTION)
                        .animateEnterExit(
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(ENTER_DURATION_MS, easing = LinearOutSlowInEasing)
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(EXIT_DURATION_MS, easing = FastOutLinearInEasing)
                            )
                        )
                        // The sheet's own height, not the container's (short sheets would
                        // overshoot), read in the layer block so a moving finger only re-places.
                        .graphicsLayer { translationY = backProgress.value * size.height }
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        // Content that swaps while the sheet is up can change its height by a row
                        // or two; animating it keeps that from landing as a jolt under the finger.
                        .animateContentSize()
                ) {
                    content()
                }
            }
        }
    }
}
