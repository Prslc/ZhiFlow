package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.preference.ReadingChoices
import com.prslc.zhiflow.ui.component.preference.GroupCornerRadius
import com.prslc.zhiflow.ui.component.preference.LevelSlider
import com.prslc.zhiflow.ui.component.preference.SettingsGroup
import com.prslc.zhiflow.ui.component.preference.SettingsRow
import com.prslc.zhiflow.ui.component.richtext.ReadingText
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * What a content page draws, reached from the settings page: how large its text is, how far apart
 * its characters and lines sit, how much room the text leaves, whether it marks the passages that
 * carry comments, and whether the page draws its progress bar and keeps its bars in place.
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
    val segmentComments by viewModel.segmentComments.collectAsStateWithLifecycle()

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
            ReadingPreview(
                textScale = textScale,
                margin = margin,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight,
                progress = readingProgress,
                segmentComments = segmentComments,
            )

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
                    ReadingSlider(
                        title = stringResource(R.string.settings_reading_margin_title),
                        valueLabel = marginLabel(margin),
                        value = margin,
                        levels = ReadingChoices.margins,
                        onValueChange = { viewModel.setReadingMargin(it) },
                        icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                    )
                }
                row {
                    SettingsRow(
                        title = stringResource(R.string.settings_reading_segment_comments_title),
                        description = stringResource(
                            R.string.settings_reading_segment_comments_summary,
                        ),
                        icon = Icons.Filled.ChatBubbleOutline,
                        onClick = { viewModel.setSegmentComments(!segmentComments) },
                        trailingContent = {
                            Switch(checked = segmentComments, onCheckedChange = null)
                        },
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
        }
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
private fun <T> ReadingSlider(
    title: String,
    valueLabel: String,
    value: T,
    levels: List<T>,
    onValueChange: (T) -> Unit,
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
 * A page drawn as the settings above would draw it: its bar, its head and a paragraph of it, at the
 * size, spacing and margins in force.
 *
 * The chrome stays schematic -- a back arrow and bars where a page puts its title and its author --
 * because it is the words the settings move, and a paragraph of them is what the reader is
 * choosing. Everything outside this block stays at the device's own size, so what changes is the
 * page and not the settings around it.
 *
 * The paragraph carries the dashed rule a commented passage carries, drawn under its lines, so the
 * switch above has something to hide.
 *
 * @param textScale The multiple of the system's size in force.
 * @param margin The room in force at either side of the text, in dp.
 * @param letterSpacing The room in force between characters, in sp.
 * @param lineHeight The multiple of the paragraph's own line height in force.
 * @param progress Whether the page draws its reading progress bar, which the preview draws too.
 * @param segmentComments Whether the page marks the passages that carry comments, which the preview
 *   draws too.
 */
@Composable
private fun ReadingPreview(
    textScale: Float,
    margin: Float,
    letterSpacing: Float,
    lineHeight: Float,
    progress: Boolean,
    segmentComments: Boolean,
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
    val skeleton = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val skeletonShape = RoundedCornerShape(4.dp)

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(GroupCornerRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column {
            if (progress) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(8.dp)
                        .clip(skeletonShape)
                        .background(skeleton),
                )
            }
            CompositionLocalProvider(LocalDensity provides sampleDensity) {
                Column(
                    modifier = Modifier.padding(
                        start = margin.dp,
                        top = 4.dp,
                        end = margin.dp,
                        bottom = 16.dp,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(16.dp)
                            .clip(skeletonShape)
                            .background(skeleton),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(skeleton),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(8.dp)
                                .clip(skeletonShape)
                                .background(skeleton),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SampleParagraph(
                        text = stringResource(R.string.settings_reading_sample),
                        style = style,
                        marked = segmentComments,
                    )
                }
            }
        }
    }
}

/**
 * The sample paragraph: [text] in [style], with the dashed rule a passage comment draws under its
 * lines when [marked].
 *
 * The rule is measured off the layout rather than painted as one line under the block: the sample
 * wraps where the size and margins in force make it wrap, and a single line would sit under the
 * last of those lines rather than follow all of them.
 *
 * @param text The sample, in the reader's language.
 * @param style The style the page's own paragraphs are drawn in.
 * @param marked Whether the paragraph carries a comment, and so draws its rule.
 */
@Composable
private fun SampleParagraph(
    text: String,
    style: TextStyle,
    marked: Boolean,
) {
    val underlineColor = MaterialTheme.colorScheme.outline
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        style = style,
        onTextLayout = { layout.value = it },
        modifier = if (marked) {
            Modifier.drawBehind {
                val textLayout = layout.value ?: return@drawBehind
                val gap = 3.dp.toPx()
                val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
                for (line in 0 until textLayout.lineCount) {
                    val y = textLayout.getLineBaseline(line) + gap
                    drawLine(
                        color = underlineColor,
                        start = Offset(textLayout.getLineLeft(line), y),
                        end = Offset(textLayout.getLineRight(line), y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash,
                    )
                }
            }
        } else {
            Modifier
        },
    )
}

/** @return The room in force at either side of the text, in dp. */
@Composable
@ReadOnlyComposable
private fun marginLabel(margin: Float): String = stringResource(
    R.string.settings_reading_margin_value,
    margin.roundToInt(),
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
