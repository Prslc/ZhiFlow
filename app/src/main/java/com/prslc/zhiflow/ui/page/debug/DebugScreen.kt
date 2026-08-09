package com.prslc.zhiflow.ui.page.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.component.preference.NavigationItemWidget
import com.prslc.zhiflow.ui.component.preference.SegmentedColumn
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
        SegmentedColumn {
            item {
                NavigationItemWidget(
                    title = stringResource(R.string.debug_item_open_link_title),
                    description = stringResource(R.string.debug_item_open_link_subtitle),
                    icon = Icons.Filled.BugReport,
                    onClick = {
                        inputUrl = ""
                        showDialog = true
                    }
                )
            }
            item {
                NavigationItemWidget(
                    title = stringResource(R.string.debug_item_config_credentials_title),
                    description = stringResource(R.string.debug_item_config_credentials_subtitle),
                    icon = Icons.Filled.BugReport,
                    onClick = {
                        showAuthDialog = true
                    }
                )
            }
        }
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text(stringResource(R.string.debug_dialog_credentials_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.authorization,
                        onValueChange = { viewModel.authorization = it },
                        label = { Text(stringResource(R.string.debug_label_authorization)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.cookie,
                        onValueChange = { viewModel.cookie = it },
                        label = { Text(stringResource(R.string.debug_label_cookie)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.xUdid,
                        onValueChange = { viewModel.xUdid = it },
                        label = { Text(stringResource(R.string.debug_label_x_udid)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.clear()
                            showAuthDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.debug_action_clear))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAuthDialog = false }) {
                            Text(stringResource(R.string.debug_action_cancel))
                        }
                        Button(
                            onClick = {
                                viewModel.save()
                                showAuthDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.debug_action_save))
                        }
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.debug_dialog_navigator_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.debug_navigator_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text(stringResource(R.string.debug_label_url)) },
                        placeholder = { Text(stringResource(R.string.debug_placeholder_url)) },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputUrl.isNotBlank()) {
                            onHandleUrl(inputUrl.trim())
                            showDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.debug_action_go))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.debug_action_cancel))
                }
            }
        )
    }
}
