package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.ProfileCoverHeader
import com.prslc.zhiflow.ui.component.common.ProfileStat
import com.prslc.zhiflow.ui.component.preference.NavigationItemWidget
import com.prslc.zhiflow.ui.component.preference.SegmentedColumn
import com.prslc.zhiflow.ui.component.preference.SegmentedColumnScope
import org.koin.androidx.compose.koinViewModel

private val GroupCornerRadius = 16.dp

/** 1dp between items lets the page background through, which reads as the group's hairline. */
private val GroupHairline = 1.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                ProfileCoverHeader(
                    user = user,
                    stats = listOf(
                        ProfileStat(stringResource(R.string.profile_stat_following), user.followingCount),
                        ProfileStat(stringResource(R.string.profile_stat_followers), user.followerCount),
                    ),
                    coverContentDescription = stringResource(R.string.profile_cover_desc),
                    topEndAction = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.profile_nav_settings),
                            )
                        }
                    },
                    actions = {
                        OutlinedButton(
                            onClick = {},
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Text(stringResource(R.string.profile_action_edit_profile))
                        }
                    },
                )

                // The first group carries no heading: its rows say what they are, and the gap
                // after the stats is break enough.
                ProfileGroup(
                    modifier = Modifier.padding(top = 16.dp),
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
                            title = stringResource(R.string.profile_nav_collections),
                            description = stringResource(R.string.profile_nav_collections_summary),
                            icon = Icons.Filled.Bookmark,
                            onClick = onNavigateToCollections,
                        )
                    }
                }

                ProfileGroup(
                    modifier = Modifier.padding(top = 4.dp),
                    title = stringResource(R.string.profile_section_social),
                ) {
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
        } else if (uiState.isLoading) {
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
private fun ProfileGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: SegmentedColumnScope.() -> Unit,
) {
    SegmentedColumn(
        modifier = modifier,
        title = title,
        cornerRadius = GroupCornerRadius,
        connectionRadius = 0.dp,
        itemGap = GroupHairline,
        itemContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        content = content,
    )
}

