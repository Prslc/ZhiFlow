package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.formatCount
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.preference.NavigationItemWidget
import com.prslc.zhiflow.ui.component.preference.SegmentedColumn
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToComments: () -> Unit,
    onNavigateToLikes: () -> Unit,
    onNavigateToCollections: (String) -> Unit,
    onNavigateToFollows: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState
    val user = uiState.user
    val isLoading = uiState.isLoading

    LaunchedEffect(Unit) {
        if (user == null) viewModel.loadProfile()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (user != null) {
            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = -statusBarHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileHeader(
                    user = user,
                    onSettingsClick = onNavigateToSettings,
                    statusBarHeight = statusBarHeight,
                )

                Spacer(modifier = Modifier.height(58.dp))

                // Name & headline
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = user.name ?: stringResource(R.string.profile_default_username),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.headline ?: stringResource(R.string.profile_default_headline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        val stats = listOf(
                            stringResource(R.string.profile_stat_following) to user.followingCount,
                            stringResource(R.string.profile_stat_followers) to user.followerCount,
                            stringResource(R.string.profile_stat_favorites) to user.favoriteCount,
                        )
                        stats.forEachIndexed { index, (label, count) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = formatCount(count),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            if (index < stats.lastIndex) {
                                VerticalDivider(
                                    modifier = Modifier.height(32.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Content section
                SegmentedColumn(
                    title = stringResource(R.string.profile_section_content),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    item {
                        NavigationItemWidget(
                            title = stringResource(R.string.profile_nav_history),
                            description = stringResource(R.string.profile_nav_history_summary),
                            icon = Icons.Filled.History,
                            onClick = onNavigateToHistory,
                        )
                    }
                    item {
                        NavigationItemWidget(
                            title = stringResource(R.string.profile_nav_comments),
                            description = stringResource(R.string.profile_nav_comments_summary),
                            icon = Icons.AutoMirrored.Filled.Chat,
                            onClick = onNavigateToComments,
                        )
                    }
                    item {
                        NavigationItemWidget(
                            title = stringResource(R.string.profile_nav_likes),
                            description = stringResource(R.string.profile_nav_likes_summary),
                            icon = Icons.Filled.ThumbUp,
                            onClick = onNavigateToLikes,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social section
                SegmentedColumn(
                    title = stringResource(R.string.profile_section_social),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    item {
                        NavigationItemWidget(
                            title = stringResource(R.string.profile_nav_collections),
                            description = stringResource(R.string.profile_nav_collections_summary),
                            icon = Icons.Filled.Bookmark,
                            onClick = { onNavigateToCollections(user.id) },
                        )
                    }
                    item {
                        NavigationItemWidget(
                            title = stringResource(R.string.profile_nav_follows),
                            description = stringResource(R.string.profile_nav_follows_summary),
                            icon = Icons.Filled.PersonAdd,
                            onClick = onNavigateToFollows,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } else if (isLoading) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            ErrorView(
                message = uiState.error?.uiMessage ?: stringResource(R.string.error_unknown),
                onRetry = { viewModel.loadProfile() },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    user: ZhihuUser,
    onSettingsClick: () -> Unit,
    statusBarHeight: Dp,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Cover image
        AsyncImage(
            model = user.coverUrl,
            contentDescription = stringResource(R.string.profile_cover_desc),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Crop,
        )

        // Settings button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusBarHeight + 8.dp, end = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.content_desc_settings),
                tint = Color.White,
            )
        }

        // Avatar & centered, overlapping the cover
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(84.dp)
                .offset(y = 42.dp),
            shape = CircleShape,
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
        ) {
            AsyncImage(
                model = user.avatar,
                contentDescription = stringResource(R.string.content_desc_avatar),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
