package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface

/**
 * Draws the single row a paged list ends with: the spinner while the next page is on its way, the
 * retry line when that attempt came back failed, the end-of-list line once there is no more.
 *
 * @param keyPrefix Prefix for the item keys, so a list can hold more than one footer.
 * @param isLoading Whether the next page is being fetched.
 * @param error The failure of the last attempt at the next page, if there was one.
 * @param onRetry What the retry line does when tapped.
 * @param endText The line drawn at the very end; null while more pages are still to come.
 */
internal fun LazyListScope.pagingFooter(
    keyPrefix: String = "footer",
    isLoading: Boolean,
    error: Throwable?,
    onRetry: () -> Unit,
    endText: String? = null,
) {
    if (isLoading) {
        item(key = "${keyPrefix}_loading", contentType = "PagingFooterLoading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingView(modifier = Modifier.size(24.dp))
            }
        }
    } else if (error != null) {
        item(key = "${keyPrefix}_error", contentType = "PagingFooterError") {
            val message = if (error is ApiException) {
                error.uiMessage
            } else {
                error.message ?: stringResource(R.string.error_unknown)
            }
            LoadMoreErrorItem(message = message, onRetry = onRetry)
        }
    } else if (endText != null) {
        item(key = "${keyPrefix}_end") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = endText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/** The two ends of a paged list: the wait, and the line that says there is no more. */
@AppPreviews
@Composable
private fun PagingFooterPreview() {
    PreviewSurface {
        Column {
            LazyColumn { pagingFooter(isLoading = true, error = null, onRetry = {}) }
            LazyColumn {
                pagingFooter(
                    isLoading = false,
                    error = null,
                    onRetry = {},
                    endText = "That is all",
                )
            }
        }
    }
}
