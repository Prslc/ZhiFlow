package com.prslc.zhiflow.ui.component.comment

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.ui.component.ImageLightbox
import com.prslc.zhiflow.ui.viewmodel.CommentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    id: String,
    contentType: ContentType,
    viewModel: CommentViewModel,
    showComments: Boolean,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    if (showComments) {
        val uiState = viewModel.uiState
        val childUiState = viewModel.childUiState
        val rootListState = rememberLazyListState()
        val childListState = rememberLazyListState()

        ModalBottomSheet(
            onDismissRequest = {
                viewModel.onSheetDismissed()
                onDismissRequest()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = { WindowInsets(0) },
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .statusBarsPadding()
            ) {

                BackHandler(enabled = uiState.isLightboxVisible) {
                    viewModel.closeImageLightbox()
                }

                BackHandler(enabled = showComments && childUiState.isDetailMode && !uiState.isLightboxVisible) {
                    viewModel.backToMain()
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
                            CommentList(
                                modifier = Modifier.weight(1f),
                                comments = uiState.comments,
                                isLoading = uiState.isLoading,
                                hasMore = uiState.hasMore,
                                onLoadMore = { viewModel.loadComments(id, contentType) },
                                onAuthorClick = { /* TODO */ },
                                onLikeClick = { commentId ->
                                    viewModel.updateCommentReaction(commentId, true)
                                },
                                onImageClick = { url -> viewModel.openImageLightbox(url) },
                                state = rootListState,
                                onShowReplies = { root ->
                                    viewModel.loadChildComments(root, forceRefresh = true)
                                }
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val currentRootId = childUiState.rootComment?.id

                            LaunchedEffect(currentRootId) {
                                if (currentRootId != null) {
                                    childListState.scrollToItem(0)
                                }
                            }

                            CommentHeader(
                                title = stringResource(R.string.comment_reply_detail),
                                onClose = { viewModel.backToMain() },
                                isBackStyle = true
                            )

                            CommentList(
                                comments = childUiState.comments,
                                isLoading = childUiState.isLoading,
                                hasMore = childUiState.hasMore,
                                rootComment = childUiState.rootComment,
                                onAuthorClick = { /* TODO */ },
                                onLikeClick = { commentId ->
                                    viewModel.updateCommentReaction(commentId, true)
                                },
                                onImageClick = { url -> viewModel.openImageLightbox(url) },
                                onLoadMore = {
                                    childUiState.rootComment?.let { root ->
                                        viewModel.loadChildComments(root, forceRefresh = false)
                                    }
                                },
                                state = childListState,
                                isChild = true,
                                modifier = Modifier.weight(1f)
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
                onDismiss = { viewModel.closeImageLightbox() }
            )
        }
    }
}

@Composable
fun CommentHeader(
    title: String,
    onClose: () -> Unit,
    isBackStyle: Boolean = false
) {
    Row(
        modifier = Modifier
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