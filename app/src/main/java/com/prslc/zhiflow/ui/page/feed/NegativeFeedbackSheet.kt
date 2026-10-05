package com.prslc.zhiflow.ui.page.feed

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.data.dto.FeedbackAction
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.widget.CustomBottomSheet
import com.prslc.zhiflow.ui.navigation.FeedbackBlockList
import com.prslc.zhiflow.ui.navigation.LocalNavigator

/**
 * Rows are whatever the backend sent, down to the wording and icons, so nothing here
 * keys off a specific module.
 */
@Composable
fun NegativeFeedbackSheet(
    state: FeedViewModel.FeedbackUiState,
    onSubmit: (FeedbackAction.Request) -> Unit,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val undeveloped = stringResource(R.string.general_undeveloped)
    val unopenable = stringResource(R.string.error_unable_to_open_link)

    val onOpenUrl: (String) -> Unit = { url ->
        val uri = url.toUri()
        // zhihu://feed/report carries the real target in zh_url, as a www.zhihu.com link.
        val reportTarget = if (uri.host == "feed" && uri.path == "/report") {
            uri.getQueryParameter("zh_url")
                ?.takeIf { it.toUri().host?.endsWith("zhihu.com") == true }
        } else null

        val blockRoute = uri.toBlockListRoute()

        when {
            reportTarget != null -> navigator.handleUrl(reportTarget)
            blockRoute != null -> navigator.navigateToFeedbackBlockList(blockRoute)
            uri.scheme == "zhihu" -> showToast(context, undeveloped)
            else -> showToast(context, unopenable)
        }
    }

    val error = state.error

    CustomBottomSheet(
        visible = state.isVisible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        when {
            state.isLoading -> LoadingView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
            )

            error != null -> ErrorView(
                message = error.uiMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )

            state.actions.isEmpty() -> Text(
                text = stringResource(R.string.feedback_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 32.dp),
            )

            // Scrolls so a long panel stays reachable at large font scales.
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
            ) {
                state.actions.forEach { action ->
                    FeedbackRow(
                        action = action,
                        onClick = {
                            when (action) {
                                is FeedbackAction.Request -> onSubmit(action)
                                is FeedbackAction.OpenUrl -> {
                                    onDismissRequest()
                                    onOpenUrl(action.url)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackRow(
    action: FeedbackAction,
    onClick: () -> Unit,
) {
    val iconUrl = if (isSystemInDarkTheme()) {
        action.nightIconUrl ?: action.iconUrl
    } else {
        action.iconUrl
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = iconUrl,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = action.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = action.maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (action.hasChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

/** Reads the block page's own parameters out of its `zhihu://feedback/block_list` link. */
private fun Uri.toBlockListRoute(): FeedbackBlockList? {
    if (host != "feedback" || path != "/block_list") return null

    return FeedbackBlockList(
        contentToken = getQueryParameter("content_token").orEmpty(),
        contentType = getQueryParameter("content_type").orEmpty(),
        feedbackType = getQueryParameter("feedback_type").orEmpty(),
    )
}
