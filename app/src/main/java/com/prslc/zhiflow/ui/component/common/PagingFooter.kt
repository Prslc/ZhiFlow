package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.uiMessage

internal fun LazyListScope.pagingFooter(
    keyPrefix: String = "footer",
    isLoading: Boolean,
    error: Throwable?,
    isEnd: Boolean = false,
    onRetry: () -> Unit
) {
    if (isLoading) {
        item(key = "${keyPrefix}_loading", contentType = "PagingFooterLoading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
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
    } else if (isEnd) {
        item(key = "${keyPrefix}_end") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_end_of_list),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
