package com.prslc.zhiflow.ui.page.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.formatCount
import com.prslc.zhiflow.data.dto.NotificationCategoryDto
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.navigation.LocalNavigator

/** How many entries fit on a line before the next one starts. */
private const val CATEGORIES_PER_ROW = 4

/**
 * The message tab: the way into each list of notifications.
 *
 * The page refreshes on the way in rather than only when it is empty: what is on screen was fetched
 * when the app started, or when the tab was last open, and either way it is older than the tab.
 *
 * @param viewModel Held by the tab rather than by this page: the badge on the tab is drawn from the
 *   same unread counts, whether or not the page has ever been shown.
 * @param modifier Applied to the column.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageScreen(
    viewModel: MessageViewModel,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val uiState = viewModel.uiState
    val categories = uiState.categories
    val globalError = uiState.globalError
    val isEmpty = categories.isEmpty()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.nav_messages),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (isEmpty && uiState.isRefreshing) {
                LoadingView(modifier = Modifier.fillMaxSize())
            } else if (isEmpty && globalError is ApiException.UnAuthorizedException) {
                GuestMessage(onSignIn = navigator::navigateToLogin)
            } else if (isEmpty && globalError != null) {
                ErrorView(
                    message = globalError.uiMessage,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (isEmpty) {
                EmptyView(
                    message = stringResource(R.string.notification_empty),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val pullState = rememberPullToRefreshState()

                PullToRefreshBox(
                    state = pullState,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.LoadingIndicator(
                            state = pullState,
                            isRefreshing = uiState.isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    },
                ) {
                    LazyColumn(
                        state = viewModel.listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "categories") {
                            CategoryRow(
                                categories = categories,
                                onClick = { category ->
                                    navigator.navigateToNotificationCategory(
                                        category.type,
                                        category.title,
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The entries into each list of notifications, four to a line as the client's own draws them. */
@Composable
private fun CategoryRow(
    categories: List<NotificationCategoryDto>,
    onClick: (NotificationCategoryDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
        categories.chunked(CATEGORIES_PER_ROW).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { category ->
                    CategoryEntry(
                        category = category,
                        onClick = { onClick(category) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Holds the last line's columns where the lines above them are.
                repeat(CATEGORIES_PER_ROW - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** One entry: its mark, its name, and how much of it is unread. */
@Composable
private fun CategoryEntry(
    category: NotificationCategoryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BadgedBox(
            badge = {
                if (category.unreadCount > 0) {
                    Badge { Text(formatCount(category.unreadCount)) }
                }
            },
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(category.type),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = category.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** What the tab shows a reader who is not signed in: the page is theirs, and it needs them. */
@Composable
private fun GuestMessage(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.message_guest_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onSignIn) {
            Text(stringResource(R.string.login_action_submit))
        }
    }
}

/** A mark for each list the server names; one it does not name is drawn as a bell. */
private fun categoryIcon(type: String): ImageVector = when (type) {
    "comment" -> Icons.AutoMirrored.Filled.Comment
    "like" -> Icons.Filled.Favorite
    "favlist_me" -> Icons.Filled.Star
    "follow" -> Icons.Filled.PersonAdd
    "system" -> Icons.Filled.Campaign
    "system_message" -> Icons.Filled.Verified
    else -> Icons.Filled.Notifications
}
