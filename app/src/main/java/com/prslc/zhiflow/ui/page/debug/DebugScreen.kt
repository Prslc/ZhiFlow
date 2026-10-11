package com.prslc.zhiflow.ui.page.debug

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.component.preference.SettingsGroup
import com.prslc.zhiflow.ui.component.preference.SettingsNavRow
import com.prslc.zhiflow.ui.component.preference.SettingsRow
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

@Composable
fun DebugScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebugViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var showDeviceIdDialog by remember { mutableStateOf(false) }
    var inputUrl by remember { mutableStateOf("") }
    var inputUdid by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.general_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Only the bottom: the sides are the group's own content padding, the same as on
                // the settings page, and a second layer here would read as a narrower page.
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            SettingsGroup {
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.debug_item_open_link_title),
                        description = stringResource(R.string.debug_item_open_link_subtitle),
                        icon = Icons.Filled.BugReport,
                        onClick = {
                            inputUrl = ""
                            showDialog = true
                        },
                    )
                }
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.debug_item_device_id_title),
                        description = stringResource(R.string.debug_item_device_id_subtitle),
                        icon = Icons.Filled.BugReport,
                        onClick = {
                            inputUdid = viewModel.xUdid
                            showDeviceIdDialog = true
                        },
                    )
                }
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.debug_item_break_tokens_title),
                        description = stringResource(R.string.debug_item_break_tokens_subtitle),
                        icon = Icons.Filled.BugReport,
                        onClick = {
                            val broken = viewModel.breakSessionTokens()
                            Toast.makeText(
                                context,
                                if (broken) {
                                    R.string.debug_break_tokens_done
                                } else {
                                    R.string.debug_refresh_unavailable
                                },
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.debug_item_force_refresh_title),
                        description = stringResource(R.string.debug_item_force_refresh_subtitle),
                        icon = Icons.Filled.BugReport,
                        onClick = {
                            val armed = viewModel.makeRefreshDue()
                            Toast.makeText(
                                context,
                                if (armed) {
                                    R.string.debug_refresh_armed
                                } else {
                                    R.string.debug_refresh_unavailable
                                },
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
                row {
                    SettingsRow(
                        title = stringResource(R.string.debug_item_http_log_title),
                        description = stringResource(R.string.debug_item_http_log_subtitle),
                        icon = Icons.Filled.BugReport,
                        onClick = viewModel::toggleLogging,
                        trailingContent = {
                            Switch(
                                checked = viewModel.loggingEnabled,
                                onCheckedChange = null,
                            )
                        },
                    )
                }
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.debug_item_http_log_view_title),
                        description = stringResource(R.string.debug_item_http_log_view_subtitle),
                        icon = Icons.Filled.BugReport,
                        onClick = { navigator.navigateToHttpLog() },
                    )
                }
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.lab_item_title),
                        description = stringResource(R.string.lab_item_subtitle),
                        icon = Icons.Filled.BugReport,
                        onClick = { navigator.navigateToRenderLab() },
                    )
                }
            }

            SettingsGroup {
                row {
                    SettingsRow(
                        title = stringResource(R.string.debug_action_lock_title),
                        description = stringResource(R.string.debug_action_lock_summary),
                        icon = Icons.Filled.Lock,
                        onClick = {
                            viewModel.setDeveloperUnlocked(false)
                            onBack()
                        },
                    )
                }
            }
        }
    }

    if (showDeviceIdDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceIdDialog = false },
            title = { Text(stringResource(R.string.debug_dialog_device_id_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputUdid,
                        onValueChange = { inputUdid = it },
                        label = { Text(stringResource(R.string.debug_label_x_udid)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { inputUdid = viewModel.regenerateUdid() },
                    ) {
                        Text(stringResource(R.string.debug_action_regenerate))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { showDeviceIdDialog = false }) {
                            Text(stringResource(R.string.debug_action_cancel))
                        }
                        Button(
                            onClick = {
                                viewModel.saveUdid(inputUdid.trim())
                                showDeviceIdDialog = false
                            },
                        ) {
                            Text(stringResource(R.string.debug_action_save))
                        }
                    }
                }
            },
            dismissButton = null,
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputUrl.isNotBlank()) {
                            navigator.handleUrl(inputUrl.trim())
                            showDialog = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.debug_action_go))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.debug_action_cancel))
                }
            },
        )
    }
}
