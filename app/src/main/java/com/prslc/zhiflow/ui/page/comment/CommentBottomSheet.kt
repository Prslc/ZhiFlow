package com.prslc.zhiflow.ui.page.comment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.component.widget.CustomBottomSheet
import com.prslc.zhiflow.ui.navigation.LocalNavigator

/**
 * The content's own comment surface: its root list, and one comment's replies behind it. The pair
 * and its pull-back live in [CommentPaneTransition], shared with a passage's panel.
 *
 * @param showComments whether the sheet is up; a back belongs to the surface only while it is up
 *   and staying, so the root list keeps the sheet's own dismissal gesture.
 */
@Composable
fun CommentBottomSheet(
    id: String,
    contentType: ContentType,
    uiState: CommentViewModel.CommentUiState,
    childUiState: CommentViewModel.ChildCommentUiState,
    showComments: Boolean,
    onEvent: (CommentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit
) {
    val navigator = LocalNavigator.current
    val rootListState = rememberLazyListState()
    val childListState = rememberLazyListState()

    LaunchedEffect(uiState.navigateToUser) {
        uiState.navigateToUser?.let {
            onDismissRequest()
            navigator.navigateToPeople(it)
            onEvent(CommentUiEvent.NavigatedToUser(it))
        }
    }

    Box {
        CustomBottomSheet(
            visible = showComments,
            modifier = modifier,
            // The header's close only asks the sheet to go; the list is cleared on the far side.
            onDismissRequest = {
                onDismissRequest()
                onEvent(CommentUiEvent.DismissSheet)
            }
        ) {
            CommentPaneTransition(
                isDetail = childUiState.isDetailMode,
                backEnabled = showComments,
                onBackToMain = { onEvent(CommentUiEvent.BackToMain) },
                main = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CommentHeader(
                            title = stringResource(R.string.comment_count, uiState.totalCount),
                            onClose = onDismissRequest
                        )
                        val onLoadMoreRoot = remember(id, contentType) {
                            { onEvent(CommentUiEvent.LoadRootComments(id, contentType)) }
                        }
                        CommentList(
                            modifier = Modifier.weight(1f),
                            onEvent = onEvent,
                            comments = uiState.comments,
                            isLoading = uiState.isLoading,
                            hasMore = uiState.hasMore,
                            onLoadMore = onLoadMoreRoot,
                            state = rootListState,
                            error = uiState.error,
                            onRetry = onLoadMoreRoot,
                        )
                    }
                },
                detail = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val currentRootId = childUiState.rootComment?.comment?.id
                        val onLoadMoreChild = remember { { onEvent(CommentUiEvent.LoadMoreReplies) } }

                        LaunchedEffect(currentRootId) {
                            if (currentRootId != null) {
                                childListState.scrollToItem(0)
                            }
                        }

                        CommentHeader(
                            title = stringResource(R.string.comment_reply_detail),
                            onClose = { onEvent(CommentUiEvent.BackToMain) },
                            isBackStyle = true,
                        )
                        CommentList(
                            modifier = Modifier.weight(1f),
                            onEvent = onEvent,
                            comments = childUiState.comments,
                            isLoading = childUiState.isLoading,
                            hasMore = childUiState.hasMore,
                            rootComment = childUiState.rootComment,
                            onLoadMore = onLoadMoreChild,
                            state = childListState,
                            isChild = true,
                            error = childUiState.error,
                            onRetry = onLoadMoreChild,
                        )
                    }
                },
            )

            // The host lives inside the sheet: a sibling would be drawn underneath it (zIndex 100).
            val snackbarHostState = rememberActionErrorHost(uiState.actionError) {
                onEvent(CommentUiEvent.ActionErrorShown)
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
fun CommentHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isBackStyle: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (isBackStyle) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.general_back),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.general_close),
                )
            }
        }
    }
}
