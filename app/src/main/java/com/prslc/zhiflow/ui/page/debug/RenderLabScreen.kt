package com.prslc.zhiflow.ui.page.debug

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.remote.parser.ContentParser
import com.prslc.zhiflow.ui.component.richtext.RichTextSingleElement
import com.prslc.zhiflow.ui.component.richtext.bodyElementPadding
import kotlin.math.roundToInt

/**
 * The font scales the knob offers, as percentages. Bounded, because a page is worth sweeping only
 * near the sizes a reader can pick, and because the seed below has to land inside it.
 */
private val FONT_SCALE_RANGE = 80..160

/**
 * A body built from the parameters on the page rather than fetched, so a case can be conjured
 * instead of hunted for. Everything here goes through the same parse and the same composables a
 * real body does.
 *
 * The font scale is one of those parameters. It is applied around the cases rather than to them,
 * as the density they are composed under, so it reaches every length the body states in sp -- the
 * text, and with it the line a formula has to fit in. A formula's picture is in dp and does not
 * follow, which is the whole reason the axis is worth a knob. The knob starts at the device's own
 * scale, so the page opens on what the app actually renders.
 *
 * It is a Compose density and nothing more: the platform's own `scaledDensity` is untouched, so a
 * case that reads `Resources` rather than a `Density` -- an Android view, a `TypedValue`, a WebView
 * -- is drawn at the device's font size whatever this says. Everything the cases draw today goes
 * through Compose, which is what makes the control honest here and only here.
 *
 * What it cannot show is anything the server decides: which segments an answer is made of, and what
 * the API says a formula's size is.
 *
 * @param onBack Called when the reader leaves the page.
 * @param modifier Applied to the `Scaffold`.
 */
@Composable
fun RenderLabScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Seeded once, from the device, because the knob means an absolute scale: starting every
    // device at 100 would open the page on a size this reader is not using. The read happens out
    // here because a `remember` calculation is not a composable scope.
    val deviceFontScale = LocalDensity.current.fontScale
    var params by remember {
        mutableStateOf(
            LabParams(
                fontScale = (deviceFontScale * 100f).roundToInt().coerceIn(FONT_SCALE_RANGE),
            ),
        )
    }
    val cases = remember(params) { labCases(params) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lab_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.general_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Knob(
                label = R.string.lab_knob_formula_height,
                value = params.formulaHeight,
                valueFormat = R.string.lab_value_dp,
                range = 8..160,
                onChange = { params = params.copy(formulaHeight = it) },
            )
            Knob(
                label = R.string.lab_knob_formula_line,
                value = params.formulaLine,
                valueFormat = R.string.lab_value_count,
                range = 1..4,
                onChange = { params = params.copy(formulaLine = it) },
            )
            Knob(
                label = R.string.lab_knob_quote_lines,
                value = params.quoteLines,
                valueFormat = R.string.lab_value_count,
                range = 1..6,
                onChange = { params = params.copy(quoteLines = it) },
            )
            Knob(
                label = R.string.lab_knob_table_cols,
                value = params.tableCols,
                valueFormat = R.string.lab_value_count,
                range = 1..6,
                onChange = { params = params.copy(tableCols = it) },
            )
            Knob(
                label = R.string.lab_knob_cell_chars,
                value = params.cellChars,
                valueFormat = R.string.lab_value_count,
                range = 2..24,
                onChange = { params = params.copy(cellChars = it) },
            )
            Knob(
                label = R.string.lab_knob_list_depth,
                value = params.listDepth,
                valueFormat = R.string.lab_value_count,
                range = 1..3,
                onChange = { params = params.copy(listDepth = it) },
            )
            Knob(
                label = R.string.lab_knob_font_scale,
                value = params.fontScale,
                valueFormat = R.string.lab_value_percent,
                range = FONT_SCALE_RANGE,
                onChange = { params = params.copy(fontScale = it) },
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp),
            )

            // The knobs stay outside this, on the device's own font scale: they are the instrument,
            // and one that resizes with what it measures is harder to read. The density is the
            // device's throughout -- only the scale moves, so a case is the same layout at another
            // reader's font size rather than a different layout.
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = params.fontScale / 100f,
                ),
            ) {
                cases.forEach { case -> LabCaseBlock(case) }
            }
        }
    }
}

@Composable
private fun Knob(
    @StringRes label: Int,
    value: Int,
    @StringRes valueFormat: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(valueFormat, value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        val track = range.first.toFloat()..range.last.toFloat()
        val state = rememberSliderState(
            value = value.toFloat(),
            steps = range.last - range.first - 1,
            trackRange = track,
        )
        Slider(
            state = state,
            // The slider draws where this state says, and a gesture only reports where it went:
            // without writing it back the thumb stays at the value the knob started at.
            onValueChange = { dragged ->
                state.value = dragged
                onChange(dragged.roundToInt())
            },
        )
    }
}

/**
 * One case: what it is, what it was built from, and the render itself. The render keeps the body's
 * own 20dp column, so a table or a formula is measured against the room it would have in an answer.
 */
@Composable
private fun LabCaseBlock(case: LabCase) {
    val elements = remember(case) { ContentParser.transform(case.segments) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(case.title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            case.note?.let { note ->
                Text(
                    text = stringResource(note, *case.noteArgs.toTypedArray()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        elements.forEach { element ->
            RichTextSingleElement(
                element = element,
                onImageClick = {},
                modifier = Modifier.padding(bodyElementPadding(element)),
            )
        }
    }
}
