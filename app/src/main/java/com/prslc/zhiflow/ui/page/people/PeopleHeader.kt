package com.prslc.zhiflow.ui.page.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.ui.component.common.FollowButton
import com.prslc.zhiflow.ui.component.common.ProfileCoverHeader
import com.prslc.zhiflow.ui.component.common.ProfileStat

/**
 * The header of someone else's profile: a message button, then the follow toggle.
 *
 * @param user Whose profile this is.
 * @param onFollowClick Called when the follow toggle is pressed.
 * @param onMessageClick Called when the message button is pressed.
 * @param onFollowersClick Called when the row's count of the people following this account is
 *   pressed.
 * @param onFollowingClick Called when its count of the people the account follows is pressed.
 * @param modifier Applied to the header, ahead of what it lays out itself.
 */
@Composable
fun PeopleHeader(
    user: ZhihuUser,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileCoverHeader(
        user = user,
        stats = listOf(
            ProfileStat(stringResource(R.string.people_stat_voteup), user.voteupCount),
            ProfileStat(
                stringResource(R.string.people_stat_followers),
                user.followerCount,
                onClick = onFollowersClick,
            ),
            ProfileStat(
                stringResource(R.string.people_stat_following),
                user.followingCount,
                onClick = onFollowingClick,
            ),
        ),
        modifier = modifier,
        actions = {
            OutlinedIconButton(
                onClick = onMessageClick,
                // M3 derives this border from the content colour, so a bright glyph would
                // otherwise drag a bright ring along with it.
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = stringResource(R.string.people_action_message),
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            FollowButton(
                isFollowing = user.isFollowing == true,
                onClick = onFollowClick,
            )
        },
    )
}
