package com.prslc.zhiflow.ui.page.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import org.koin.androidx.compose.koinViewModel

@Composable
fun DebugScreen(
    onHandleUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebugViewModel = koinViewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var inputUrl by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        DebugItem(
            title = "Open Link",
            subtitle = "Paste a Zhihu URL to test LinkParser",
            onClick = {
                inputUrl = ""
                showDialog = true
            }
        )
        DebugItem(
            title = "Configure Credentials",
            subtitle = "Set custom request headers at runtime",
            onClick = {
                showAuthDialog = true
            }
        )
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("Configure Credentials") },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.authorization,
                        onValueChange = { viewModel.authorization = it },
                        label = { Text("Authorization") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.cookie,
                        onValueChange = { viewModel.cookie = it },
                        label = { Text("Cookie") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.xUdid,
                        onValueChange = { viewModel.xUdid = it },
                        label = { Text("x_udid") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.save()
                        showAuthDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.clear()
                        showAuthDialog = false
                    }
                ) {
                    Text("Clear")
                }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Debug Navigator") },
            text = {
                Column {
                    Text(
                        text = "Enter any URL (Answer, Article, or External)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("URL") },
                        placeholder = { Text("https://www.zhihu.com/question/...") },
                        singleLine = false,
                        maxLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputUrl.isNotBlank()) {
                            onHandleUrl(inputUrl.trim())
                            showDialog = false
                        }
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DebugItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(Icons.Default.BugReport, null) },
        modifier = modifier.clickable(onClick = onClick),
    )
}
