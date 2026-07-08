/*
 * Copyright (C) 2024 The Miuix Project Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Rewritten using Material Design 3 ListItem.
 */

package com.prslc.zhiflow.ui.component.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ArrowPreference(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = summary?.let {
            { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        },
        leadingContent = startAction,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                endActions()

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        modifier = modifier.clickable(enabled = enabled && onClick != null) {
            onClick?.invoke()
        },
        colors = ListItemDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}