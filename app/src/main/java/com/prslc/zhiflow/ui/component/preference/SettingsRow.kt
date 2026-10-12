package com.prslc.zhiflow.ui.component.preference

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface

private const val DISABLED_ALPHA = 0.38f

private val RowIconSize = 24.dp

/** A row's own description sits this far behind its title. */
private const val DESCRIPTION_ALPHA = 0.7f

/**
 * How far a row's text is held off its own top and bottom, in dp at the reader's font scale: the
 * padding grows with the system font so a larger setting does not crowd the row.
 */
@Composable
@ReadOnlyComposable
private fun rowInternalPadding(): Dp = with(LocalDensity.current) { (4 * fontScale).dp }

/**
 * One row of a [SettingsGroup]: an icon, a title, an optional description, and whatever the
 * caller puts at its end.
 *
 * The row draws no background and no corner of its own -- the group clips it and paints behind
 * it, so a row would show as bare text on its own.
 *
 * @param title The row's headline.
 * @param onClick Called when the row is tapped.
 * @param modifier Applied to the row.
 * @param description Supporting line under the title; blank draws none.
 * @param icon Leading icon; null leaves the title flush with the row's start.
 * @param enabled False dims the row and stops it responding to taps.
 * @param trailingContent Drawn at the row's end, such as a switch or a chevron; null draws none.
 */
@Composable
fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "",
    icon: ImageVector? = null,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val internalPadding = rowInternalPadding()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    val descriptionColor = contentColor.copy(alpha = DESCRIPTION_ALPHA)

    ListItem(
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        onClick = onClick,
        enabled = enabled,
        verticalAlignment = Alignment.CenterVertically,
        interactionSource = LocalSettingsRowInteraction.current,
        shapes = ListItemDefaults.shapes(
            shape = RectangleShape,
            pressedShape = RectangleShape,
            selectedShape = RectangleShape,
            focusedShape = RectangleShape,
            hoveredShape = RectangleShape,
            draggedShape = RectangleShape,
        ),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            leadingContentColor = iconColor,
            trailingContentColor = iconColor,
            supportingContentColor = descriptionColor,
            // The row's alpha already carries the disabled look, so these must not dim as well,
            // or a disabled row would end up dimmed twice.
            disabledContainerColor = Color.Transparent,
            disabledContentColor = contentColor,
            disabledLeadingContentColor = iconColor,
            disabledTrailingContentColor = iconColor,
            disabledSupportingContentColor = descriptionColor,
        ),
        leadingContent = if (icon == null) {
            null
        } else {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(RowIconSize),
                )
            }
        },
        supportingContent = if (description.isEmpty()) {
            null
        } else {
            {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = internalPadding),
                )
            }
        },
        trailingContent = trailingContent,
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    top = internalPadding,
                    bottom = if (description.isEmpty()) internalPadding else 0.dp,
                ),
            )
        },
    )
}

/**
 * A [SettingsRow] that opens something, marked by a chevron at its end.
 *
 * @param title The row's headline.
 * @param onClick Called when the row is tapped.
 * @param modifier Applied to the row.
 * @param description Supporting line under the title; blank draws none.
 * @param icon Leading icon; null leaves the title flush with the row's start.
 * @param enabled False dims the row and stops it responding to taps.
 */
@Composable
fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "",
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SettingsRow(
        title = title,
        onClick = onClick,
        modifier = modifier,
        description = description,
        icon = icon,
        enabled = enabled,
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
            )
        },
    )
}

/** A bare row, a row with a description, a disabled one, and the one that opens something. */
@AppPreviews
@Composable
private fun SettingsRowPreview() {
    PreviewSurface {
        SettingsGroup(title = "Reading") {
            row { SettingsNavRow(title = "Read history", onClick = {}) }
            row {
                SettingsNavRow(
                    title = "Collections",
                    description = "What you have saved, and where",
                    onClick = {},
                )
            }
            row { SettingsRow(title = "Data saver", description = "Off", onClick = {}) }
            row {
                SettingsRow(
                    title = "Developer tools",
                    description = "Needs ten taps on the version row",
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}
