package com.prslc.zhiflow.ui.page.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.utils.platform.rememberCopyTextToClipboard
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.component.widget.CustomBottomSheet
import com.prslc.zhiflow.ui.theme.SectionGapDark
import com.prslc.zhiflow.ui.theme.SectionGapLight
import kotlinx.coroutines.launch

/**
 * How tall the panel stands: the official one measures 77% of the screen and holds it whatever the
 * comment count. Filling the available height instead leaves a strip of empty sheet under four
 * comments.
 */
private const val PANEL_HEIGHT_FRACTION = 0.77f

/**
 * The panel a `seg_like` range opens: the passage it covers, what can be done with it, and the
 * comments left on it.
 *
 * The body holds on to the last range it was given: one that rendered only while the range is
 * non-null would collapse to an empty panel in the frame a dismissal clears it, and the exit would
 * play on nothing.
 *
 * @param onToggleLike takes the range's key, so the like state stays with the screen that owns the
 *   body rather than with the panel.
 * @param actionError failures of either the passage like or a comment like; the sheet carries the
 *   only host that is visible while it is up.
 */
@Composable
fun SegmentPanelSheet(
    target: SegmentLikeTarget?,
    comments: CommentViewModel.SegmentUiState,
    onToggleLike: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    actionError: ApiException? = null,
    onErrorConsumed: () -> Unit = {},
    onEvent: (CommentUiEvent) -> Unit = {},
) {
    var held by remember { mutableStateOf(target) }
    if (target != null) held = target

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val copyText = rememberCopyTextToClipboard()

    Box(modifier = modifier) {
        CustomBottomSheet(
            visible = target != null,
            onDismissRequest = onDismissRequest,
            heightFraction = PANEL_HEIGHT_FRACTION,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                held?.let { range ->
                    Text(
                        text = range.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )

                    SegmentActionRow(
                        likeCount = range.likeCount,
                        isLiked = range.isLiked,
                        commentCount = comments.totalCount,
                        onCopy = { copyText(range.text) },
                        onLike = { onToggleLike(range.key) },
                        onComments = { scope.launch { listState.animateScrollToItem(0) } },
                    )

                    HorizontalDivider(
                        thickness = 6.dp,
                        color = if (isSystemInDarkTheme()) SectionGapDark else SectionGapLight,
                    )
                }

                CommentList(
                    modifier = Modifier.weight(1f),
                    onEvent = onEvent,
                    comments = comments.comments,
                    isLoading = comments.isLoading,
                    hasMore = comments.hasMore,
                    onLoadMore = onLoadMore,
                    state = listState,
                    error = comments.error,
                    onRetry = onRetry,
                )
            }

            val snackbarHostState = rememberActionErrorHost(actionError, onErrorConsumed)
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * The row of things one passage can be used for.
 *
 * The like button reports the passage's own count whether or not the reader liked it; only the icon
 * and its colour follow the reader. At zero there is no count to report and it names the action
 * instead, the way the comment rows drop a zero.
 *
 * Share and search are drawn but inert: they lead to surfaces this app does not have yet, and the
 * passage is where the reader expects to find them.
 */
@Composable
private fun SegmentActionRow(
    likeCount: Int,
    isLiked: Boolean,
    commentCount: Int,
    onCopy: () -> Unit,
    onLike: () -> Unit,
    onComments: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SegmentAction(
            icon = Icons.Outlined.ContentCopy,
            label = stringResource(R.string.segment_action_copy),
            onClick = onCopy,
        )
        SegmentAction(
            icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            label = if (likeCount > 0) {
                likeCount.toString()
            } else {
                stringResource(R.string.segment_action_like)
            },
            tint = if (isLiked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onLike,
        )
        SegmentAction(
            icon = Icons.AutoMirrored.Outlined.Comment,
            label = commentCount.toString(),
            onClick = onComments,
        )
        SegmentAction(
            icon = Icons.Default.Share,
            label = stringResource(R.string.segment_action_share),
            onClick = {},
        )
        SegmentAction(
            icon = Icons.Default.Search,
            label = stringResource(R.string.segment_action_search),
            onClick = {},
        )
    }
}

@Composable
private fun SegmentAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}
