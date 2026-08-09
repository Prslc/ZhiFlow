/*
 * Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
 * This file includes code derived from https://github.com/wxxsfxyzm/InstallerX-Revived
 * Modified: Adapted for ZhiFlow Material3 widgets
 */

package com.prslc.zhiflow.ui.component.preference

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A setting item that navigates to a secondary page, built upon BaseWidget.
 * It includes an icon, title, description, and a trailing arrow.
 *
 * @param title The main title text of the item.
 * @param modifier The modifier to be applied to the widget.
 * @param icon The leading icon for the item.
 * @param iconPlaceholder If true, maintains a consistent leading space even when [icon] is null.
 * @param description The supporting description text.
 * @param enabled Controls the enabled state of the widget.
 * @param onClick The callback to be invoked when this item is clicked.
 */
@Composable
fun NavigationItemWidget(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = true,
    description: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    BaseWidget(
        modifier = modifier,
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        description = description,
        enabled = enabled,
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null
        )
    }
}
