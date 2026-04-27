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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R

import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.LoadingView

@Composable
fun CommentList(
    modifier: Modifier = Modifier,
    comments: List<CommentViewModel.CommentUiModel>, // 修改类型
    rootComment: CommentViewModel.CommentUiModel? = null,
    isLoading: Boolean,             // track loading state
    hasMore: Boolean,               // check if pagination ended
    onLoadMore: () -> Unit,         // trigger next page
    state: LazyListState,
    isChild: Boolean = false,
    onEvent: (CommentEvent) -> Unit = {}
) {

    val stateTarget = when {
        isLoading && comments.isEmpty() -> "LOADING"
        comments.isNotEmpty() -> "CONTENT"
        else -> "EMPTY"
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
                                        isChild = false,
                                        showReplyButton = false,
                                        onEvent = onEvent
                                    )
                                    HorizontalDivider(
                                        thickness = 4.dp,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = comments,
                            key = { _, item -> item.comment.id }
                        ) { index, model ->

                            // load more
                            if (index >= comments.size - 3 && !isLoading && hasMore) {
                                LaunchedEffect(comments.size) {
                                    onLoadMore()
                                }
                            }
                            CommentItem(
                                model = model,
                                isChild = isChild,
                                onEvent = onEvent
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }

                        if (isLoading && comments.isNotEmpty()) {
                            item(key = "loading_footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }

                "LOADING" -> {
                    LoadingView(
                        modifier = modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    )
                }

                "EMPTY" -> {
                    EmptyView(
                        message = if (isChild) stringResource(R.string.comment_empty_child) else stringResource(
                            R.string.comment_empty_root
                        ),
                        modifier = modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    )
                }
            }
        }
    }
}