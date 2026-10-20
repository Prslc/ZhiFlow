package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.preference.ReadingMargin
import com.prslc.zhiflow.core.preference.ReadingTextSize
import com.prslc.zhiflow.ui.component.preference.ChoiceDialog
import com.prslc.zhiflow.ui.component.preference.GroupCornerRadius
import com.prslc.zhiflow.ui.component.preference.SettingsGroup
import com.prslc.zhiflow.ui.component.preference.SettingsNavRow
import com.prslc.zhiflow.ui.component.preference.SettingsRow
import org.koin.androidx.compose.koinViewModel

/**
 * What a content page draws along its edges, reached from the settings page: how large its text is,
 * how much room the text leaves, and whether the page draws its progress bar and keeps its bars in
 * place.
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
    val textSize by viewModel.readingTextSize.collectAsStateWithLifecycle()
    val margin by viewModel.readingMargin.collectAsStateWithLifecycle()
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showMarginDialog by remember { mutableStateOf(false) }

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
                    SettingsNavRow(
                        title = stringResource(R.string.settings_reading_text_size_title),
                        description = textSize.label(),
                        icon = Icons.Filled.FormatSize,
                        onClick = { showTextSizeDialog = true },
                    )
                }
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.settings_reading_margin_title),
                        description = margin.label(),
                        icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                        onClick = { showMarginDialog = true },
                    )
                }
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

            ReadingSample(textSize = textSize, margin = margin)
        }
    }

    if (showTextSizeDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_reading_text_size_title),
            options = ReadingTextSize.entries,
            selected = textSize,
            label = { it.label() },
            onSelect = {
                viewModel.setReadingTextSize(it)
                showTextSizeDialog = false
            },
            onDismiss = { showTextSizeDialog = false },
        )
    }

    if (showMarginDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_reading_margin_title),
            options = ReadingMargin.entries,
            selected = margin,
            label = { it.label() },
            onSelect = {
                viewModel.setReadingMargin(it)
                showMarginDialog = false
            },
            onDismiss = { showMarginDialog = false },
        )
    }
}

/**
 * A paragraph drawn the way a content page draws its own, at the size and the margins in force: the
 * rows above stay at the device's own size, and so does everything outside this block, so that what
 * changes is the sample and not the page around the controls.
 *
 * @param textSize The size in force, as a multiple of the system's own.
 * @param margin The room in force at either side of the text.
 */
@Composable
private fun ReadingSample(textSize: ReadingTextSize, margin: ReadingMargin) {
    val density = LocalDensity.current
    val sampleDensity = remember(density, textSize) {
        Density(density = density.density, fontScale = density.fontScale * textSize.scale)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(GroupCornerRadius),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        CompositionLocalProvider(LocalDensity provides sampleDensity) {
            Text(
                text = stringResource(R.string.settings_reading_sample),
                // The paragraph's own style, so the sample lines break where a page's would.
                style = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.25.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = margin.horizontalDp.dp, vertical = 16.dp),
            )
        }
    }
}

/** @return How the reader's language writes this text size. */
@Composable
@ReadOnlyComposable
private fun ReadingTextSize.label(): String = stringResource(
    when (this) {
        ReadingTextSize.Small -> R.string.settings_reading_text_size_small
        ReadingTextSize.Standard -> R.string.settings_reading_text_size_standard
        ReadingTextSize.Large -> R.string.settings_reading_text_size_large
        ReadingTextSize.ExtraLarge -> R.string.settings_reading_text_size_extra_large
    },
)

/** @return How the reader's language writes this margin. */
@Composable
@ReadOnlyComposable
private fun ReadingMargin.label(): String = stringResource(
    when (this) {
        ReadingMargin.Narrow -> R.string.settings_reading_margin_narrow
        ReadingMargin.Standard -> R.string.settings_reading_margin_standard
        ReadingMargin.Wide -> R.string.settings_reading_margin_wide
    },
)
