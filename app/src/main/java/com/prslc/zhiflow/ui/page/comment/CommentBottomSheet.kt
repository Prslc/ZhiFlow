package com.prslc.zhiflow.ui.page.comment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface

/**
 * The content's own comment surface: its root list, and one comment's replies behind it. The pair
 * and its pull-back live in [CommentPaneTransition], shared with a passage's panel.
 *
 * The box is not furniture of the sheet: it is up only where the reader asked for it, and the way
 * in to a comment of their own is a FAB over the list.
 *
 * @param uiState The section's state, as a producer: the reader's draft changes on every keystroke,
 *   and a value the caller read would recompose the page around the sheet with it.
 * @param childUiState The reply pane's state, read here for the same reason.
 * @param showComments Whether the sheet is up; a back belongs to the surface only while it is up
 *   and staying, so the root list keeps the sheet's own dismissal gesture.
 * @param canComment Whether the reader may write here at all, which the content's own payload and
 *   the comment section's say between them. Where they do not, the way in is not offered.
 */
@Composable
fun CommentBottomSheet(
    id: String,
    contentType: ContentType,
    uiState: () -> CommentViewModel.CommentUiState,
    childUiState: () -> CommentViewModel.ChildCommentUiState,
    showComments: Boolean,
    canComment: () -> Boolean,
    onEvent: (CommentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
) {
    val state = uiState()
    val childState = childUiState()
    val canCommentNow = canComment()

    val navigator = LocalNavigator.current
    val rootListState = rememberLazyListState()
    val childListState = rememberLazyListState()

    // The reader opens over the sheet, which stays up underneath and is still there on the way
    // back. A token that arrives with this sheet down belongs to the passage panel, which opens it
    // itself; two sheets navigating would push the reader twice.
    LaunchedEffect(state.navigateToUser) {
        val token = state.navigateToUser ?: return@LaunchedEffect
        if (!showComments) return@LaunchedEffect
        navigator.navigateToPeople(token)
        onEvent(CommentUiEvent.NavigatedToUser(token))
    }

    Box {
        CustomBottomSheet(
            visible = showComments,
            modifier = modifier,
            // The header's close only asks the sheet to go; the list is cleared on the far side.
            onDismissRequest = {
                onDismissRequest()
                onEvent(CommentUiEvent.DismissSheet)
            },
        ) {
            CommentPaneTransition(
                isDetail = childState.isDetailMode,
                backEnabled = showComments,
                onBackToMain = { onEvent(CommentUiEvent.BackToMain) },
                main = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                    ) {
                        CommentHeader(
                            title = stringResource(R.string.comment_count, state.totalCount),
                            onClose = onDismissRequest,
                        )
                        val onLoadMoreRoot = remember(id, contentType) {
                            { onEvent(CommentUiEvent.LoadRootComments(id, contentType)) }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CommentList(
                                modifier = Modifier.fillMaxSize(),
                                onEvent = onEvent,
                                comments = state.comments,
                                isLoading = state.isLoading,
                                hasMore = state.hasMore,
                                onLoadMore = onLoadMoreRoot,
                                state = rootListState,
                                error = state.error,
                                onRetry = onLoadMoreRoot,
                            )

                            // The way into a comment on the content itself. It is not a box: the
                            // box is asked for, so what floats here is only the asking.
                            CommentComposeFab(
                                visible = canCommentNow && !state.isComposerOpen,
                                listState = rootListState,
                                onClick = { onEvent(CommentUiEvent.OpenComposer) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                            )
                        }

                        // The reader has just commented: what they wrote was put at the top of this
                        // list, and a list left where it was would not show it.
                        LaunchedEffect(state.lastPostedId) {
                            if (state.lastPostedId != null) rootListState.animateScrollToItem(0)
                        }

                        if (state.isComposerOpen) {
                            CommentComposer(
                                draft = state.draft,
                                isSubmitting = state.isSubmitting,
                                // Answering one of the comments above, where one of them was tapped;
                                // otherwise a comment on the content itself.
                                replyToName = childState.replyTarget?.author?.name,
                                onDraftChange = { onEvent(CommentUiEvent.DraftChanged(it)) },
                                onSend = { onEvent(CommentUiEvent.SubmitComment(id, contentType)) },
                                onClose = { onEvent(CommentUiEvent.CloseComposer) },
                            )
                        }
                    }
                },
                detail = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                    ) {
                        val rootComment = childState.rootComment
                        val currentRootId = rootComment?.comment?.id
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
                            comments = childState.comments,
                            isLoading = childState.isLoading,
                            hasMore = childState.hasMore,
                            rootComment = childState.rootComment,
                            onLoadMore = onLoadMoreChild,
                            state = childListState,
                            isChild = true,
                            error = childState.error,
                            onRetry = onLoadMoreChild,
                        )

                        // The reply goes on the end of this list, which is where a new one belongs
                        // and usually where the reader is not. The pane draws the comment being
                        // answered first, so the replies start at one and the last is the size.
                        LaunchedEffect(childState.lastPostedId) {
                            if (childState.lastPostedId != null) {
                                childListState.animateScrollToItem(childState.comments.size)
                            }
                        }

                        // The box is up only where something in the pane was tapped to answer it,
                        // and it says whom in its hint.
                        val replyTarget = childState.replyTarget
                        if (state.isComposerOpen && replyTarget != null) {
                            CommentComposer(
                                draft = state.draft,
                                isSubmitting = state.isSubmitting,
                                replyToName = replyTarget.author.name,
                                onDraftChange = { onEvent(CommentUiEvent.DraftChanged(it)) },
                                onSend = { onEvent(CommentUiEvent.SubmitComment(id, contentType)) },
                                onClose = { onEvent(CommentUiEvent.CloseComposer) },
                            )
                        }
                    }
                },
            )

            // The host lives inside the sheet: a sibling would be drawn underneath it (zIndex 100).
            val snackbarHostState = rememberActionErrorHost(state.actionError) {
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
    isBackStyle: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        if (isBackStyle) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(start = 4.dp),
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
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.general_close),
                )
            }
        }
    }
}

/** The sheet's own heading, as it reads over the content and as it reads over a passage. */
@AppPreviews
@Composable
private fun CommentHeaderPreview() {
    PreviewSurface {
        Column {
            CommentHeader(title = "96 comments", onClose = {})
            CommentHeader(title = "96 comments", onClose = {}, isBackStyle = true)
        }
    }
}
