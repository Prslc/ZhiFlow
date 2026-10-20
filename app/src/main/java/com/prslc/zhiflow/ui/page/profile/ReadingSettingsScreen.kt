package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.SpaceBar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.preference.ReadingChoices
import com.prslc.zhiflow.core.preference.ReadingMargin
import com.prslc.zhiflow.ui.component.preference.ChoiceDialog
import com.prslc.zhiflow.ui.component.preference.GroupCornerRadius
import com.prslc.zhiflow.ui.component.preference.LevelSlider
import com.prslc.zhiflow.ui.component.preference.SettingsGroup
import com.prslc.zhiflow.ui.component.preference.SettingsNavRow
import com.prslc.zhiflow.ui.component.preference.SettingsRow
import com.prslc.zhiflow.ui.component.richtext.ReadingText
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * What a content page draws along its edges, reached from the settings page: how large its text is,
 * how far apart its characters and lines sit, how much room the text leaves, and whether the page
 * draws its progress bar and keeps its bars in place.
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
    val textScale by viewModel.readingTextScale.collectAsStateWithLifecycle()
    val letterSpacing by viewModel.readingLetterSpacing.collectAsStateWithLifecycle()
    val lineHeight by viewModel.readingLineHeight.collectAsStateWithLifecycle()
    val margin by viewModel.readingMargin.collectAsStateWithLifecycle()
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
                    ReadingSlider(
                        title = stringResource(R.string.settings_reading_text_size_title),
                        valueLabel = textSizeLabel(textScale),
                        value = textScale,
                        levels = ReadingChoices.textScales,
                        onValueChange = { viewModel.setReadingTextScale(it) },
                        icon = Icons.Filled.FormatSize,
                    )
                }
                row {
                    ReadingSlider(
                        title = stringResource(R.string.settings_reading_letter_spacing_title),
                        valueLabel = letterSpacingLabel(letterSpacing),
                        value = letterSpacing,
                        levels = ReadingChoices.letterSpacings,
                        onValueChange = { viewModel.setReadingLetterSpacing(it) },
                        icon = Icons.Filled.SpaceBar,
                    )
                }
                row {
                    ReadingSlider(
                        title = stringResource(R.string.settings_reading_line_height_title),
                        valueLabel = lineHeightLabel(lineHeight),
                        value = lineHeight,
                        levels = ReadingChoices.lineHeights,
                        onValueChange = { viewModel.setReadingLineHeight(it) },
                        icon = Icons.Filled.FormatLineSpacing,
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

            ReadingSample(
                textScale = textScale,
                margin = margin,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight,
            )
        }
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
 * One of the reading settings: its name and the value in force above, and the track that sets it
 * below, in the shape the other rows already use.
 *
 * @param title What the slider sets, drawn above it and read out by the track.
 * @param valueLabel How the value in force is written.
 * @param value The level in force, one of [levels].
 * @param levels The levels on offer, in the order the track fills towards them.
 * @param onValueChange Called with the level nearest the finger as it moves.
 * @param icon The mark drawn beside the name.
 */
@Composable
private fun ReadingSlider(
    title: String,
    valueLabel: String,
    value: Float,
    levels: List<Float>,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LevelSlider(
            label = title,
            value = value,
            levels = levels,
            onValueChange = onValueChange,
        )
    }
}

/**
 * A paragraph drawn the way a content page draws its own, at the size, spacing and margins in
 * force: the rows above stay at the device's own size, and so does everything outside this block,
 * so that what changes is the sample and not the page around the controls.
 *
 * @param textScale The multiple of the system's size in force.
 * @param margin The room in force at either side of the text.
 * @param letterSpacing The room in force between characters, in sp.
 * @param lineHeight The multiple of the paragraph's own line height in force.
 */
@Composable
private fun ReadingSample(
    textScale: Float,
    margin: ReadingMargin,
    letterSpacing: Float,
    lineHeight: Float,
) {
    val density = LocalDensity.current
    val sampleDensity = remember(density, textScale) {
        Density(density = density.density, fontScale = density.fontScale * textScale)
    }
    val reading = remember(letterSpacing, lineHeight) {
        ReadingText(letterSpacing = letterSpacing, lineHeight = lineHeight)
    }
    // The paragraph's own style, so the sample lines break where a page's would.
    val style = reading.applyTo(
        MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.25.sp),
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(GroupCornerRadius),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        CompositionLocalProvider(LocalDensity provides sampleDensity) {
            Text(
                text = stringResource(R.string.settings_reading_sample),
                style = style,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = margin.horizontalDp.dp, vertical = 16.dp),
            )
        }
    }
}

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

/** @return The text size in force, as the percentage of the system's own that it draws at. */
@Composable
@ReadOnlyComposable
private fun textSizeLabel(scale: Float): String =
    stringResource(R.string.settings_reading_text_size_value, (scale * 100).roundToInt())

/** @return The room in force between characters, signed to show which way it moves. */
@Composable
@ReadOnlyComposable
private fun letterSpacingLabel(spacing: Float): String =
    stringResource(R.string.settings_reading_letter_spacing_value, spacing)

/** @return The multiple of a style's own line height in force. */
@Composable
@ReadOnlyComposable
private fun lineHeightLabel(scale: Float): String =
    stringResource(R.string.settings_reading_line_height_value, scale)
