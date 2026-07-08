package com.prslc.zhiflow.ui.component.preference

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class PreferenceItem(
    val title: String,
    val summary: String? = null,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
fun PreferenceGroup(
    items: List<PreferenceItem>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        items.forEachIndexed { index, item ->
            ArrowPreference(
                title = item.title,
                summary = item.summary,
                startAction = { Icon(item.icon, contentDescription = null) },
                onClick = item.onClick,
                enabled = item.enabled,
            )
            if (index < items.lastIndex) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}
