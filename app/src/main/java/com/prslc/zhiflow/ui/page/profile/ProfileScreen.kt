package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.ui.component.common.CoverStatusBarIconEffect
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.ProfileCoverHeader
import com.prslc.zhiflow.ui.component.common.ProfileCoverHeight
import com.prslc.zhiflow.ui.component.common.ProfileStat
import com.prslc.zhiflow.ui.component.preference.NavigationItemWidget
import com.prslc.zhiflow.ui.component.preference.SegmentedColumn
import com.prslc.zhiflow.ui.component.preference.SegmentedColumnScope
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

private val GroupCornerRadius = 16.dp

/** 1dp between items lets the page background through, which reads as the group's hairline. */
private val GroupHairline = 1.dp

/** The profile an anonymous page draws: no cover, no avatar, and the header's own defaults. */
private val GuestUser = ZhihuUser()

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
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState
    val user = uiState.user
    val navigator = LocalNavigator.current
    var showSignOutDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (!uiState.isGuest && user != null) {
            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val scrollState = rememberScrollState()

            CoverStatusBarIconEffect(
                scrollState = scrollState,
                coverHeight = ProfileCoverHeight,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = -statusBarHeight)
                    .verticalScroll(scrollState),
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

                ProfileContentGroups(
                    enabled = true,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToCollections = onNavigateToCollections,
                    onNavigateToComments = onNavigateToComments,
                    onNavigateToLikes = onNavigateToLikes,
                    onNavigateToFollows = onNavigateToFollows,
                )

                ProfileGroup(
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    item {
                        NavigationItemWidget(
                            title = stringResource(R.string.profile_action_sign_out),
                            description = stringResource(R.string.profile_action_sign_out_summary),
                            icon = Icons.AutoMirrored.Filled.Logout,
                            onClick = { showSignOutDialog = true },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } else if (uiState.isGuest) {
            // The page above with its content locked, not a page of its own: what signing in
            // opens should be visible from here.
            // The guest's own profile when the server has answered with it, and a bare one
            // until then — the header reads what it is given and fills the rest itself.
            val guest = user ?: GuestUser
            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val scrollState = rememberScrollState()

            CoverStatusBarIconEffect(
                scrollState = scrollState,
                coverHeight = ProfileCoverHeight,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = -statusBarHeight)
                    .verticalScroll(scrollState),
            ) {
                ProfileCoverHeader(
                    user = guest,
                    stats = listOf(
                        ProfileStat(
                            stringResource(R.string.profile_stat_following),
                            guest.followingCount,
                        ),
                        ProfileStat(
                            stringResource(R.string.profile_stat_followers),
                            guest.followerCount,
                        ),
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
                        Button(onClick = navigator::navigateToLogin) {
                            Text(stringResource(R.string.login_action_submit))
                        }
                    },
                )

                Text(
                    text = stringResource(R.string.profile_guest_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp),
                )

                ProfileContentGroups(
                    modifier = Modifier.padding(bottom = 32.dp),
                    enabled = false,
                    onNavigateToHistory = {},
                    onNavigateToCollections = {},
                    onNavigateToComments = {},
                    onNavigateToLikes = {},
                    onNavigateToFollows = {},
                )
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

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.profile_sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.profile_sign_out_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    },
                ) {
                    Text(stringResource(R.string.profile_action_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.profile_sign_out_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileContentGroups(
    enabled: Boolean,
    onNavigateToHistory: () -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToComments: () -> Unit,
    onNavigateToLikes: () -> Unit,
    onNavigateToFollows: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // The first group carries no heading: its rows say what they are, and the gap above is
        // break enough.
        ProfileGroup(
            modifier = Modifier.padding(top = 16.dp),
        ) {
            item {
                NavigationItemWidget(
                    title = stringResource(R.string.profile_nav_history),
                    description = stringResource(R.string.profile_nav_history_summary),
                    icon = Icons.Filled.History,
                    enabled = enabled,
                    onClick = onNavigateToHistory,
                )
            }
            item {
                NavigationItemWidget(
                    title = stringResource(R.string.profile_nav_collections),
                    description = stringResource(R.string.profile_nav_collections_summary),
                    icon = Icons.Filled.Bookmark,
                    enabled = enabled,
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
                    enabled = enabled,
                    onClick = onNavigateToComments,
                )
            }
            item {
                NavigationItemWidget(
                    title = stringResource(R.string.profile_nav_likes),
                    description = stringResource(R.string.profile_nav_likes_summary),
                    icon = Icons.Filled.ThumbUp,
                    enabled = enabled,
                    onClick = onNavigateToLikes,
                )
            }
            item {
                NavigationItemWidget(
                    title = stringResource(R.string.profile_nav_follows),
                    description = stringResource(R.string.profile_nav_follows_summary),
                    icon = Icons.Filled.PersonAdd,
                    enabled = enabled,
                    onClick = onNavigateToFollows,
                )
            }
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
