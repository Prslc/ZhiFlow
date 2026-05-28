package com.prslc.zhiflow.ui.page.comment

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.ui.component.widget.ImageLightbox
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    if (showComments) {
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

        ModalBottomSheet(
            onDismissRequest = {
                onEvent(CommentUiEvent.DismissSheet)
                onDismissRequest()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = { WindowInsets(0) },
            dragHandle = null
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .statusBarsPadding()
            ) {

                BackHandler(enabled = uiState.isLightboxVisible) {
                    onEvent(CommentUiEvent.CloseImage)
                }

                BackHandler(enabled = showComments && childUiState.isDetailMode && !uiState.isLightboxVisible) {
                    onEvent(CommentUiEvent.BackToMain)
                }

                AnimatedContent(
                    targetState = childUiState.isDetailMode,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally { it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "CommentSheetTransition"
                ) { isDetail ->
                    if (!isDetail) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CommentHeader(
                                title = stringResource(R.string.comment_count, uiState.totalCount),
                                onClose = {
                                    scope.launch { sheetState.hide() }
                                        .invokeOnCompletion { onDismissRequest() }
                                }
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
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val currentRootId = childUiState.rootComment?.comment?.id

                            LaunchedEffect(currentRootId) {
                                if (currentRootId != null) {
                                    childListState.scrollToItem(0)
                                }
                            }

                            CommentHeader(
                                title = stringResource(R.string.comment_reply_detail),
                                onClose = { onEvent(CommentUiEvent.BackToMain) },
                                isBackStyle = true
                            )

                            val onLoadMoreChild = remember { { onEvent(CommentUiEvent.LoadMoreReplies) } }
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
                            )
                        }
                    }
                }
            }
        }
        if (uiState.isLightboxVisible) {
            ImageLightbox(
                imageUrls = uiState.selectedImageUrls,
                initialIndex = uiState.initialImageIndex,
                onDismiss = { onEvent(CommentUiEvent.CloseImage) }
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
                    contentDescription = "back"
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "close"
                )
            }
        }
    }
}
