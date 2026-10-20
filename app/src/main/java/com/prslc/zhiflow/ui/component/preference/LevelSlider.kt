package com.prslc.zhiflow.ui.component.preference

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import kotlin.math.roundToInt

/** The track's height, which the thumb and the stops are sized from. */
private val TrackHeight = 28.dp

/** The thumb, as a fraction of the track's half height. */
private const val THUMB_RADIUS_RATIO = 0.72f

/** The thumb's size while a finger is on the track. */
private const val THUMB_PRESSED_SCALE = 1.127f

/** How much of the track's half height a stop's radius is. */
private const val STOP_RADIUS_DIVISOR = 7.5f

/** The room the track's fill gives up while it is being dragged. */
private const val PRESS_OVERLAY_ALPHA = 0.044f

/**
 * A level, set by dragging a track that fills towards the thumb, with a mark at every level it
 * offers.
 *
 * The thumb and the marks keep half the track's height clear of either end, so the end levels sit
 * in the middle of the track's rounded caps rather than against its edges. A drag takes the level
 * under the finger once it starts and carries it from there; the press is consumed, so a finger
 * that means to move the level does not scroll the page instead.
 *
 * The shape follows Miuix's slider (Apache-2.0, github.com/compose-miuix-ui/miuix): a 28dp
 * track, a thumb of 0.72 of its half height, stops of a seventh of it, and the fill drawn
 * round-capped so that it reaches half the track's height past the thumb. The form is theirs; what
 * is drawn here and how the finger moves it are ours.
 *
 * @param label What the control sets, read out rather than drawn: the caller draws the name above.
 * @param value The level in force, one of [levels].
 * @param levels The levels on offer, in the order the track fills towards them.
 * @param onValueChange Called with the level nearest the finger as it moves.
 * @param modifier Applied to the track.
 */
@Composable
fun LevelSlider(
    label: String,
    value: Float,
    levels: List<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = levels.indexOf(value).coerceAtLeast(0)
    val steps = (levels.size - 1).coerceAtLeast(1)
    val haptics = LocalHapticFeedback.current
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var lastChosen by remember { mutableIntStateOf(-1) }
    var isDragging by remember { mutableStateOf(false) }

    fun chooseAt(x: Float) {
        val height = trackSize.height.toFloat()
        if (height <= 0f || trackSize.width <= 0) return
        val radius = height / 2f
        val usable = (trackSize.width - height).coerceAtLeast(1f)
        val fraction = ((x - radius) / usable).coerceIn(0f, 1f)
        val nearest = (fraction * steps).roundToInt().coerceIn(0, levels.lastIndex)
        if (nearest != lastChosen) {
            lastChosen = nearest
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onValueChange(levels[nearest])
        }
    }

    // The thumb moves with a finger's own stiffness while it is held, and settles on the level
    // with a softer one once it is let go.
    val fraction by animateFloatAsState(
        targetValue = index.toFloat() / steps,
        animationSpec = if (isDragging) {
            spring(dampingRatio = 0.9f, stiffness = 1755f)
        } else {
            spring(dampingRatio = 0.96f, stiffness = 322f)
        },
        label = "LevelSliderFraction",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) THUMB_PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 987f),
        label = "LevelSliderThumb",
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (isDragging) PRESS_OVERLAY_ALPHA else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "LevelSliderPress",
    )

    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val fillColor = MaterialTheme.colorScheme.primary
    val thumbColor = MaterialTheme.colorScheme.onPrimary
    val reachedStopColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val emptyStopColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .clip(CircleShape)
            .onSizeChanged { trackSize = it }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    dragOffset += delta
                    chooseAt(dragOffset)
                },
                onDragStarted = { offset ->
                    isDragging = true
                    lastChosen = -1
                    dragOffset = offset.x
                    chooseAt(offset.x)
                },
                onDragStopped = { isDragging = false },
            )
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = index.toFloat(),
                    range = 0f..steps.toFloat(),
                    steps = (levels.size - 2).coerceAtLeast(0),
                )
                setProgress { target ->
                    val nearest = target.roundToInt().coerceIn(0, levels.lastIndex)
                    onValueChange(levels[nearest])
                    true
                }
            },
    ) {
        val height = size.height
        val radius = height / 2f
        val usable = (size.width - height).coerceAtLeast(0f)
        val thumbX = radius + usable * fraction
        val stopRadius = height / STOP_RADIUS_DIVISOR
        val corner = CornerRadius(radius)

        drawRoundRect(color = trackColor, cornerRadius = corner)
        drawRoundRect(color = Color.Black, alpha = pressAlpha, cornerRadius = corner)
        drawRoundRect(
            color = fillColor,
            size = Size((thumbX + radius).coerceAtMost(size.width), height),
            cornerRadius = corner,
        )
        levels.forEachIndexed { level, _ ->
            val stopX = radius + usable * (level.toFloat() / steps)
            drawCircle(
                color = if (stopX <= thumbX) reachedStopColor else emptyStopColor,
                radius = stopRadius,
                center = Offset(stopX, height / 2f),
            )
        }
        drawCircle(
            color = thumbColor,
            radius = radius * THUMB_RADIUS_RATIO * thumbScale,
            center = Offset(thumbX, height / 2f),
        )
    }
}

/** One track at its lowest, its middle and its highest, and the marks between them. */
@AppPreviews
@Composable
private fun LevelSliderPreview() {
    PreviewSurface {
        LevelSlider(
            label = "Level",
            value = 0.5f,
            levels = listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            onValueChange = {},
        )
    }
}
