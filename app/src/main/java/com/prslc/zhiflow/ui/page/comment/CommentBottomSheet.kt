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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.component.widget.CustomBottomSheet
import com.prslc.zhiflow.ui.navigation.LocalNavigator
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
 * The sheet's two panes -- the root list and one root comment's replies -- are a single seekable
 * transition, so the back gesture drives the exit under the finger, and the header's arrow, the back
 * key and a released gesture all land on the same specs: the app's own pop language (MainActivity).
 *
 * Two traps come with that shape. A drive has to be re-issued rather than issued once: the framework
 * hands its mutator mutex between callers, so a gesture's seek, the next intent or a pull-back
 * cancels whichever drive is running, and a cancelled drive leaves the panes part-way across with
 * nobody left to finish them. The stall watch is what repairs that.
 *
 * And the intent has to be read through `rememberUpdatedState`: [childUiState] is a parameter, a
 * value rather than a state holder, so anything outliving a recomposition would keep reading the
 * object it was composed with.
 *
 * @param showComments whether the sheet is up; the back handler is enabled only in detail mode, so
 *   the root list keeps the sheet's own dismissal gesture.
 */
@Composable
fun CommentBottomSheet(
    id: String,
    contentType: ContentType,
    uiState: CommentViewModel.CommentUiState,
    childUiState: CommentViewModel.ChildCommentUiState,
    showComments: Boolean,
    onEvent: (CommentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit
) {
    val navigator = LocalNavigator.current
    val rootListState = rememberLazyListState()
    val childListState = rememberLazyListState()

    LaunchedEffect(uiState.navigateToUser) {
        uiState.navigateToUser?.let {
            onDismissRequest()
            navigator.navigateToPeople(it)
            onEvent(CommentUiEvent.NavigatedToUser(it))
        }
    }

    Box {
        CustomBottomSheet(
            visible = showComments,
            modifier = modifier,
            onDismissRequest = {
                onEvent(CommentUiEvent.DismissSheet)
                onDismissRequest()
            }
        ) {
            val isDetailMode = childUiState.isDetailMode

            val detailState = remember { SeekableTransitionState(isDetailMode) }
            val detailTransition = rememberTransition(detailState, label = "CommentSheetDetail")
            val scope = rememberCoroutineScope()
            val detailIntent by rememberUpdatedState(childUiState.isDetailMode)
            var isGesturing by remember { mutableStateOf(false) }

            LaunchedEffect(isDetailMode) {
                if (detailState.targetState != isDetailMode) {
                    if (isDetailMode) detailState.animateTo(true) else detailState.completeExit()
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

            PredictiveBackHandler(enabled = showComments && isDetailMode) { progress ->
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
                onEvent(CommentUiEvent.BackToMain)
            }

            detailTransition.AnimatedContent(
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        // The incoming pane is placed on top, so the outgoing one fades as it
                        // leaves -- otherwise its content would show through the arriving list.
                        (slideInHorizontally(EXIT_MOVE) { -it / 5 } + fadeIn(EXIT_FADE)) togetherWith
                                (slideOutHorizontally(EXIT_MOVE) { it } + fadeOut(EXIT_FADE))
                    }
                }
            ) { isDetail ->
                if (!isDetail) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CommentHeader(
                            title = stringResource(R.string.comment_count, uiState.totalCount),
                            onClose = {
                                onEvent(CommentUiEvent.DismissSheet)
                                onDismissRequest()
                            }
                        )
                        val onLoadMoreRoot = remember(id, contentType) {
                            { onEvent(CommentUiEvent.LoadRootComments(id, contentType)) }
                        }
                        CommentList(
                            modifier = Modifier.weight(1f),
                            onEvent = onEvent,
                            comments = uiState.comments,
                            isLoading = uiState.isLoading,
                            hasMore = uiState.hasMore,
                            onLoadMore = onLoadMoreRoot,
                            state = rootListState,
                            error = uiState.error,
                            onRetry = onLoadMoreRoot,
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val currentRootId = childUiState.rootComment?.comment?.id
                        val onLoadMoreChild = remember { { onEvent(CommentUiEvent.LoadMoreReplies) } }

                        LaunchedEffect(currentRootId) {
                            if (currentRootId != null) {
                                childListState.scrollToItem(0)
                            }
                        }

                        CommentHeader(
                            title = stringResource(R.string.comment_reply_detail),
                            onClose = { onEvent(CommentUiEvent.BackToMain) },
                            isBackStyle = true,
                        )
                        CommentList(
                            modifier = Modifier.weight(1f),
                            onEvent = onEvent,
                            comments = childUiState.comments,
                            isLoading = childUiState.isLoading,
                            hasMore = childUiState.hasMore,
                            rootComment = childUiState.rootComment,
                            onLoadMore = onLoadMoreChild,
                            state = childListState,
                            isChild = true,
                            error = childUiState.error,
                            onRetry = onLoadMoreChild,
                        )
                    }
                }
            }

            // The host lives inside the sheet: a sibling would be drawn underneath it (zIndex 100).
            val snackbarHostState = rememberActionErrorHost(uiState.actionError) {
                onEvent(CommentUiEvent.ActionErrorShown)
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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

@Composable
fun CommentHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isBackStyle: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (isBackStyle) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.general_back),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.general_close),
                )
            }
        }
    }
}
