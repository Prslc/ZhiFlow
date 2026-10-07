package com.prslc.zhiflow.core.utils.compose

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt

/**
 * [progress] is the share of the scroll behind the reader: 0 at the top of the list, 1 when it
 * bottoms out, and 0 when there is nothing to scroll. [passed] is the share of the body above the
 * viewport's top edge, or 1 once the item after the body has come into view.
 */
@Immutable
data class ReadingPosition(
    val progress: Float,
    val passed: Float,
)

internal fun LazyListLayoutInfo.readingPosition(bodyEnd: Int): ReadingPosition {
    if (bodyEnd <= 1) return ReadingPosition(0f, 0f)
    val first = visibleItemsInfo.firstOrNull() ?: return ReadingPosition(0f, 0f)
    val last = visibleItemsInfo.last()

    val passedFirst = if (first.size > 0) -first.offset.toFloat() / first.size else 0f
    val scrolledItems = first.index + passedFirst
    val passed = if (last.index >= bodyEnd) 1f
    else (scrolledItems / bodyEnd).coerceIn(0f, 1f)

    // Both edges are read through the item they fall inside; as whole items they would step, and a
    // divisor that steps moves the ratio against the scroll.
    val seenLast = if (last.size > 0) {
        ((viewportEndOffset - last.offset).toFloat() / last.size).coerceIn(0f, 1f)
    } else 1f
    val belowFold = (totalItemsCount - 1 - last.index) + (1f - seenLast)

    val scrollable = scrolledItems + belowFold
    val progress = if (scrollable > 0f) (scrolledItems / scrollable).coerceIn(0f, 1f) else 0f

    return ReadingPosition(progress, passed)
}

/**
 * Reports nothing until [bodyComplete]: a half-published body ends at its last published item, which
 * [readingPosition] cannot tell from the end of the body.
 */
@Composable
internal fun ReadingProgressEffect(
    state: LazyListState,
    bodyEnd: Int,
    bodyComplete: Boolean,
    onProgress: (ReadingPosition) -> Unit,
) {
    val report by rememberUpdatedState(onProgress)
    LaunchedEffect(state, bodyEnd, bodyComplete) {
        if (!bodyComplete) return@LaunchedEffect
        snapshotFlow { state.layoutInfo.readingPosition(bodyEnd) }.collect { report(it) }
    }
}

@Stable
internal class ReadingProgress {

    /** What the bar draws, from [ReadingPosition.progress]. */
    var fraction by mutableFloatStateOf(0f)
        private set

    private var passed = 0f

    /** Latched: once the end has been on screen the item stays reported as read. */
    private var reachedEnd = false

    fun update(position: ReadingPosition) {
        fraction = position.progress
        passed = position.passed
        if (position.passed >= 1f) reachedEnd = true
    }

    /**
     * The percent to store, or 0 when there is nothing to store: an untouched page has no progress,
     * and writing 0 over an item the user had already read into would clear it.
     */
    fun reportedPercent(): Int = if (reachedEnd) 100 else (passed * 100).roundToInt()

    fun reset() {
        fraction = 0f
        passed = 0f
        reachedEnd = false
    }
}

/** Reports on the way out, from either exit: the app pausing, or the screen leaving the back stack. */
@Composable
internal fun FlushProgressOnLeave(onFlush: () -> Unit) {
    val flush by rememberUpdatedState(onFlush)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            flush()
        }
    }
}
