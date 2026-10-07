package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "TODO",
                )
            }
        }
    }
}
