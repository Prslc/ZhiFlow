package com.prslc.zhiflow.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun DebugScreen(
    innerPadding: PaddingValues,
    onNavigateToAnswer: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var answerId by remember { mutableStateOf("") }

    Column(Modifier.padding(innerPadding).padding(16.dp)) {
        ListItem(
            headlineContent = { Text("测试回答页面") },
            supportingContent = { Text("通过 Answer ID 直接跳转") },
            leadingContent = { Icon(Icons.Default.QuestionAnswer, null) },
            modifier = Modifier.clickable { showDialog = true }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("跳转至指定回答") },
            text = {
                Column {
                    Text("请输入回答 ID (例如: 1998880095331443030)",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = answerId,
                        onValueChange = { answerId = it },
                        label = { Text("Answer ID") },
                        singleLine = true,
                        placeholder = { Text("输入 ID...") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (answerId.isNotBlank()) {
                            onNavigateToAnswer(answerId)
                            showDialog = false
                        }
                    }
                ) {
                    Text("跳转")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}