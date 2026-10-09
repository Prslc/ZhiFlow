package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.component.widget.CustomBottomSheet

/** What a row's text keeps from the sheet's edges, and what each level of depth adds to it. */
private const val ROW_INSET_DP = 20
private const val ROW_LEVEL_INDENT_DP = 16

/** The bar marking the row being read, and the room the indent leaves for it. */
private val CURRENT_RAIL_WIDTH = 3.dp
private val CURRENT_RAIL_GAP = 12.dp

// The second level and the chevrons step back by opacity rather than by taking the scheme's variant
// tones: on the dynamic palette this runs on, onSurfaceVariant measured within 2/255 of onSurface,
// so a step written in tones is not there to see.
private const val SECOND_LEVEL_ALPHA = 0.7f

// A row's chevron is the one mark that repeats on every row, so it has to sit far enough down that
// it is read only when looked for: at the text's own half-strength it was a column of its own.
private const val CHEVRON_ALPHA = 0.25f

/**
 * The outline of a content body: its headings, indented by how deep each sits, the one the reader
 * is in marked, and a tap that takes them to one. The screen owns the sheet, this only draws it.
 *
 * @param visible Whether the sheet is up. The screen owns the flag; this only draws what it says.
 * @param entries The headings to list, already indented by the outline that produced them.
 * @param currentIndex Hands back the entry being read, and is taken as a lambda so that the read
 * happens here: as a value it would be read in the screen's scope, and every heading the reader
 * crosses would recompose the whole screen.
 * @param onEntryClick Called with the entry the reader picked. Scrolling the body is the screen's.
 * @param onDismissRequest Called when the sheet is sent away, by a back or by a tap outside it.
 * @param modifier Applied to the sheet itself, so it reaches the surface and the scrim under it.
 */
@Composable
fun BodyOutlineSheet(
    visible: Boolean,
    entries: List<OutlineEntry>,
    currentIndex: () -> Int?,
    onEntryClick: (OutlineEntry) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val current = currentIndex()

    // Opens at the entry being read rather than at the top of a long outline. Only on the way in:
    // the body cannot scroll while the sheet is up, so nothing else would move it.
    LaunchedEffect(visible) {
        if (visible && current != null) listState.scrollToItem(current)
    }

    CustomBottomSheet(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.outline_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.general_close),
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // Not weight(1f) on its own: filling the share it is given is what would hold a short
            // outline's sheet up at the height cap instead of letting it sit at its own height.
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                itemsIndexed(entries) { index, entry ->
                    val isCurrent = index == current
                    val topLevel = entry.depth == 0
                    val indent = (ROW_INSET_DP + entry.depth * ROW_LEVEL_INDENT_DP).dp
                    val railColor = MaterialTheme.colorScheme.primary
                    val levelStyle = if (topLevel) {
                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    } else {
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                                .copy(alpha = SECOND_LEVEL_ALPHA),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Marks the row, not only the words: the blue says where the reader
                            // is in the text, this says which row they are about to tap.
                            .drawBehind {
                                if (!isCurrent) return@drawBehind
                                val railWidth = CURRENT_RAIL_WIDTH.toPx()
                                drawRoundRect(
                                    color = railColor,
                                    topLeft = Offset(
                                        indent.toPx() - CURRENT_RAIL_GAP.toPx(),
                                        0f,
                                    ),
                                    size = Size(railWidth, size.height),
                                    cornerRadius = CornerRadius(railWidth / 2f),
                                )
                            }
                            .clickable { onEntryClick(entry) }
                            .padding(
                                start = indent,
                                end = ROW_INSET_DP.dp,
                                top = 12.dp,
                                bottom = 12.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ZRichText(
                            content = entry.content,
                            inlineMetas = entry.inlineMetas,
                            style = levelStyle.copy(
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    levelStyle.color
                                },
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                                .copy(alpha = CHEVRON_ALPHA),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
