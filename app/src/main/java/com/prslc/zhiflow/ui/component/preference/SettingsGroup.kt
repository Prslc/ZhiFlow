package com.prslc.zhiflow.ui.component.preference

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/** Corner of a group's own outline. */
val GroupCornerRadius = 16.dp

/** Corner two neighboring rows share, which rounds each row's inner end. */
val GroupConnectionRadius = 5.dp

/** Space left between two rows, which is where the page shows through as a hairline. */
val GroupRowGap = 2.dp

private val GroupContentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

private val GroupTitlePadding = PaddingValues(start = 16.dp, top = 8.dp, bottom = 16.dp)

/**
 * The interaction source of the row currently being composed under a [SettingsGroup].
 *
 * The group hands its own source down so that the row's taps are reported to the group, which is
 * what animates the row's outline while a finger is down -- only the group knows the corner that
 * outline starts from. Null outside a group, where a row keeps a source of its own.
 */
@SuppressLint("ComposeCompositionLocalUsage")
val LocalSettingsRowInteraction = staticCompositionLocalOf<MutableInteractionSource?> { null }

@DslMarker
annotation class SettingsGroupDsl

/**
 * Collects the rows of a [SettingsGroup].
 *
 * A row is written as its content alone: the corner it is clipped to, the color behind it and
 * the gap to its neighbor are all decided by the group, which is the only one that knows where
 * in the stack the row sits.
 */
@SettingsGroupDsl
class SettingsGroupScope {

    internal val rows = mutableListOf<@Composable () -> Unit>()

    /** @param content The row, drawn inside the shape and color the group gives it. */
    fun row(content: @Composable () -> Unit) {
        rows.add(content)
    }
}

/**
 * A block of settings rows, drawn as one rounded surface with the page showing between them.
 *
 * Each row is clipped to a shape that morphs towards the group's own corner while it is held
 * down. The group draws that shape rather than leaving it to the row's `ListItem`, whose own
 * shape morph lerps against the motion scheme's spring unfloored -- under the expressive scheme
 * its overshoot rounds a corner past flat, which an outline rejects.
 *
 * @param modifier Applied to the group, ahead of its own content padding.
 * @param title Heading drawn above the rows; blank draws none.
 * @param cornerRadius Corner of the group's own outline.
 * @param connectionRadius Corner two neighboring rows share.
 * @param rowGap Space left between two rows.
 * @param containerColor Color behind each row.
 * @param contentPadding Padding around the whole group.
 * @param content The rows, declared in order.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    cornerRadius: Dp = GroupCornerRadius,
    connectionRadius: Dp = GroupConnectionRadius,
    rowGap: Dp = GroupRowGap,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentPadding: PaddingValues = GroupContentPadding,
    content: SettingsGroupScope.() -> Unit,
) {
    val rows = SettingsGroupScope().apply(content).rows
    if (rows.isEmpty()) return
    val lastIndex = rows.lastIndex

    Column(modifier = modifier.padding(contentPadding)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(GroupTitlePadding),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
            rows.forEachIndexed { index, row ->
                key(index) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()
                    val pressProgress by animateFloatAsState(
                        targetValue = if (pressed) 1f else 0f,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        label = "RowPress",
                    )

                    val topRadius = if (index == 0) cornerRadius else connectionRadius
                    val bottomRadius = if (index == lastIndex) cornerRadius else connectionRadius
                    // Floored only: the expressive spring overshoots, and that overshoot is what
                    // makes the corner round out rather than stop.
                    val progress = pressProgress.coerceAtLeast(0f)

                    val shape = RoundedCornerShape(
                        topStart = lerp(topRadius, cornerRadius, progress),
                        topEnd = lerp(topRadius, cornerRadius, progress),
                        bottomStart = lerp(bottomRadius, cornerRadius, progress),
                        bottomEnd = lerp(bottomRadius, cornerRadius, progress),
                    )

                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(containerColor),
                    ) {
                        CompositionLocalProvider(
                            LocalSettingsRowInteraction provides interactionSource,
                        ) {
                            row()
                        }
                    }
                }
            }
        }
    }
}
