package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.component.widget.CustomBottomSheet

/**
 * The outline of a content body: its headings, indented by how deep each sits, the one the reader is
 * in marked, and a tap that takes them to one. The screen owns the sheet, this only draws it.
 *
 * @param currentIndex hands back the entry being read, and is taken as a lambda so that the read
 * happens here: as a value it would be read in the screen's scope, and every heading the reader
 * crosses would recompose the whole screen.
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
        Column(modifier = Modifier.fillMaxSize()) {
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

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(entries) { index, entry ->
                    val isCurrent = index == current
                    ZRichText(
                        content = entry.content,
                        inlineMetas = entry.inlineMetas,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEntryClick(entry) }
                            .padding(
                                start = (20 + entry.depth * 16).dp,
                                end = 20.dp,
                                top = 12.dp,
                                bottom = 12.dp,
                            ),
                    )
                }
            }
        }
    }
}
