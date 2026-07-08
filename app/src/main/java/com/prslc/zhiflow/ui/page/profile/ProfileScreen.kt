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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.preference.PreferenceGroup
import com.prslc.zhiflow.ui.component.preference.PreferenceItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToComments: () -> Unit,
    onNavigateToLikes: () -> Unit,
    onNavigateToCollections: () -> Unit,
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

                Spacer(modifier = Modifier.height(54.dp))

                // Name & headline
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = user.name ?: stringResource(R.string.profile_default_username),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = user.headline ?: stringResource(R.string.profile_default_headline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Content section
                SectionLabel(
                    text = stringResource(R.string.profile_section_content),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                PreferenceGroup(
                    items = listOf(
                        PreferenceItem(
                            title = stringResource(R.string.profile_nav_history),
                            summary = stringResource(R.string.profile_nav_history_summary),
                            icon = Icons.Filled.History,
                            onClick = onNavigateToHistory,
                        ),
                        PreferenceItem(
                            title = stringResource(R.string.profile_nav_comments),
                            summary = stringResource(R.string.profile_nav_comments_summary),
                            icon = Icons.AutoMirrored.Filled.Chat,
                            onClick = onNavigateToComments,
                        ),
                        PreferenceItem(
                            title = stringResource(R.string.profile_nav_likes),
                            summary = stringResource(R.string.profile_nav_likes_summary),
                            icon = Icons.Filled.ThumbUp,
                            onClick = onNavigateToLikes,
                        ),
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Social section
                SectionLabel(
                    text = stringResource(R.string.profile_section_social),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                PreferenceGroup(
                    items = listOf(
                        PreferenceItem(
                            title = stringResource(R.string.profile_nav_collections),
                            summary = stringResource(R.string.profile_nav_collections_summary),
                            icon = Icons.Filled.Bookmark,
                            onClick = onNavigateToCollections,
                        ),
                        PreferenceItem(
                            title = stringResource(R.string.profile_nav_follows),
                            summary = stringResource(R.string.profile_nav_follows_summary),
                            icon = Icons.Filled.PersonAdd,
                            onClick = onNavigateToFollows,
                        ),
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        } else if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                .height(160.dp),
            contentScale = ContentScale.Crop,
        )

        // Settings button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusBarHeight + 4.dp, end = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.content_desc_settings),
                tint = Color.White,
            )
        }

        // Avatar + inline stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp)
                .offset(y = 42.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Avatar with border ring
            Surface(
                modifier = Modifier.size(84.dp),
                shape = CircleShape,
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
            ) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = stringResource(R.string.content_desc_avatar),
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Inline stats
            Row(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Bottom),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                ProfileStat(
                    label = stringResource(R.string.profile_stat_following),
                    count = user.followingCount,
                )
                ProfileStat(
                    label = stringResource(R.string.profile_stat_followers),
                    count = user.followerCount,
                )
                ProfileStat(
                    label = stringResource(R.string.profile_stat_favorites),
                    count = user.favoriteCount,
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    label: String,
    count: Int,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp),
    )
}
