package com.prslc.zhiflow.ui.page.comment

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.pagingFooter

@Composable
fun CommentList(
    onEvent: (CommentUiEvent) -> Unit,
    comments: List<CommentViewModel.CommentUiModel>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    state: LazyListState,
    modifier: Modifier = Modifier,
    rootComment: CommentViewModel.CommentUiModel? = null,
    isChild: Boolean = false,
    error: ApiException? = null,
    onRetry: () -> Unit = {}
) {
    val errorMessage = error?.uiMessage
    val loadMoreError = if (comments.isNotEmpty()) error else null
    val stateTarget = when {
        isLoading && comments.isEmpty() -> "LOADING"
        errorMessage != null && comments.isEmpty() -> "ERROR"
        comments.isNotEmpty() -> "CONTENT"
        else -> "EMPTY"
    }

    // Hoisted out of the items: an effect living inside a LazyColumn item is re-launched
    // whenever that item is recomposed, which would retry forever on a failing page.
    val shouldLoadMore by remember { state.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasMore && !isLoading && loadMoreError == null) onLoadMore()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = stateTarget,
            contentKey = { it },
            transitionSpec = {
                fadeIn().togetherWith(fadeOut())
            },
            label = "CommentListStatusTransition"
        ) { target ->
            when (target) {
                "CONTENT" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = state
                    ) {
                        // Pin
                        if (rootComment != null) {
                            item(key = "root_${rootComment.comment.id}") {
                                Column {
                                    CommentItem(
                                        model = rootComment,
                                        onEvent = onEvent,
                                        isChild = false,
                                        showReplyButton = false,
                                    )
                                    HorizontalDivider(
                                        thickness = 4.dp,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = comments,
                            key = { _, item -> item.comment.id }
                        ) { _, model ->
                            CommentItem(
                                model = model,
                                onEvent = onEvent,
                                isChild = isChild,
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }

                        pagingFooter(
                            keyPrefix = if (isChild) "child" else "root",
                            isLoading = isLoading,
                            error = loadMoreError,
                            onRetry = onLoadMore,
                        )
                    }
                }

                "LOADING" -> {
                    LoadingView(modifier = Modifier.fillMaxSize())
                }

                "ERROR" -> {
                    ErrorView(
                        message = errorMessage ?: stringResource(R.string.error_unknown),
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                "EMPTY" -> {
                    EmptyView(
                        message = if (isChild) stringResource(R.string.comment_empty_child) else stringResource(
                            R.string.comment_empty_root
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    )
                }
            }
        }
    }
}
