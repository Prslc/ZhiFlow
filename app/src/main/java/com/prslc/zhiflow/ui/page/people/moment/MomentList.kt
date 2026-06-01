package com.prslc.zhiflow.ui.page.people.moment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.data.model.MediaImage
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadMoreErrorItem
import com.prslc.zhiflow.ui.component.common.LoadingView

@Immutable
enum class MomentContentType {
    ANSWER,
    ARTICLE,
    THOUGHT,
    USER,
    UNKNOWN
}

@Immutable
data class MomentItemState(
    val id: String,
    val type: MomentContentType,
    val title: String,
    val plainContent: String,
    val summary: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val routerUrl: String?,
    val voteCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val mediaImages: List<MediaImage>,
    val videoThumbnail: String?,
    val publishedAt: Long,
    val actionText: String,
    val actionTime: Long,
    val isTopping: Boolean,
)

fun LazyListScope.momentsContent(
    urlToken: String,
    state: MomentViewModel.MomentUiState,
    viewModel: MomentViewModel<*>
) {
    val prefix = viewModel.tabKeyPrefix

    when {
        state.isLoading && state.moments.isEmpty() -> {
            item(key = "${prefix}_initial_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingView()
                }
            }
        }

        state.error != null && state.moments.isEmpty() -> {
            item(key = "${prefix}_initial_error") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorView(
                        message = state.error.uiMessage,
                        onRetry = { viewModel.loadMoment(urlToken) },
                    )
                }
            }
        }

        !state.isLoading && state.moments.isEmpty() -> {
            item(key = "${prefix}_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.people_works_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        else -> {
            items(
                items = state.moments,
                key = { item -> "${prefix}_${item.id}" },
                contentType = { item -> item.type }
            ) { item ->
                when (item.type) {
                    MomentContentType.USER -> UserMomentCard(state = item)
                    else -> StandardMomentCard(state = item)
                }
            }

            pagingFooter(
                keyPrefix = prefix,
                isLoading = state.isNextLoading,
                error = state.error.takeIf { state.moments.isNotEmpty() },
                onRetry = { viewModel.loadMore() },
            )
        }
    }
}

private fun LazyListScope.pagingFooter(
    keyPrefix: String,
    isLoading: Boolean,
    error: Throwable?,
    onRetry: () -> Unit
) {
    if (isLoading) {
        item(key = "${keyPrefix}_footer_loading") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LoadingView(modifier = Modifier.size(24.dp))
            }
        }
    } else if (error != null) {
        item(key = "${keyPrefix}_footer_error") {
            val message = if (error is ApiException) error.uiMessage else error.message
                ?: stringResource(R.string.error_unknown)
            LoadMoreErrorItem(message = message, onRetry = onRetry)
        }
    }
}
