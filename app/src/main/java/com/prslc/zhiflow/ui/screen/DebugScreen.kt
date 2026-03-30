package com.prslc.zhiflow.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DebugScreen(
    onNavigateToContent: (String, String) -> Unit
) {
    var activeDialogType by remember { mutableStateOf<String?>(null) }
    var inputId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        DebugItem(
            title = "Test Answer Page",
            subtitle = "Jump directly via Answer ID",
            onClick = {
                activeDialogType = "answer"
                inputId = ""
            }
        )

        DebugItem(
            title = "Test Article Page",
            subtitle = "Jump directly via Article ID",
            onClick = {
                activeDialogType = "article"
                inputId = ""
            }
        )
    }

    // Dialog
    activeDialogType?.let { type ->
        val isArticle = type == "article"
        val label = if (isArticle) "Article" else "Answer"

        AlertDialog(
            onDismissRequest = { activeDialogType = null },
            title = { Text("Navigate to $label") },
            text = {
                Column {
                    Text(
                        text = "Enter the target $label ID below",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputId,
                        onValueChange = { inputId = it },
                        label = { Text("$label ID") },
                        placeholder = { Text("e.g. 123456") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputId.isNotBlank()) {
                            onNavigateToContent(inputId, type)
                            activeDialogType = null
                        }
                    }
                ) {
                    Text("Navigate")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDialogType = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DebugItem(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(Icons.Default.BugReport, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}