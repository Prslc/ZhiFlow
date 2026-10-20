package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.component.preference.SettingsGroup
import com.prslc.zhiflow.ui.component.preference.SettingsRow
import org.koin.androidx.compose.koinViewModel

/**
 * What a content page draws along its edges, reached from the settings page.
 *
 * @param onBack Called when the reader leaves the page.
 */
@Composable
fun ReadingSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReadingSettingsViewModel = koinViewModel(),
) {
    val readingProgress by viewModel.readingProgress.collectAsStateWithLifecycle()
    val barsFollowScroll by viewModel.barsFollowScroll.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_reading_title)) },
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
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsGroup {
                row {
                    SettingsRow(
                        title = stringResource(R.string.settings_reading_progress_title),
                        description = stringResource(R.string.settings_reading_progress_summary),
                        icon = Icons.Filled.LinearScale,
                        onClick = { viewModel.setReadingProgress(!readingProgress) },
                        trailingContent = {
                            Switch(checked = readingProgress, onCheckedChange = null)
                        },
                    )
                }
                row {
                    SettingsRow(
                        title = stringResource(R.string.settings_bars_follow_scroll_title),
                        description = stringResource(R.string.settings_bars_follow_scroll_summary),
                        icon = Icons.Filled.OpenInFull,
                        onClick = { viewModel.setBarsFollowScroll(!barsFollowScroll) },
                        trailingContent = {
                            Switch(checked = barsFollowScroll, onCheckedChange = null)
                        },
                    )
                }
            }
        }
    }
}
