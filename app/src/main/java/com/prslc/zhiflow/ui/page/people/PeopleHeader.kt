package com.prslc.zhiflow.ui.page.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.ui.component.common.FollowButton
import com.prslc.zhiflow.ui.component.common.StatItem
import com.prslc.zhiflow.ui.component.widget.rememberSingleImageLightbox

@Composable
fun PeopleHeader(
    user: ZhihuUser,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openCover = rememberSingleImageLightbox(user.coverUrl)
    val openAvatar = rememberSingleImageLightbox(user.avatar)
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = user.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clickable(onClick = openCover),
                contentScale = ContentScale.Crop,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp)
                    .offset(y = 42.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Surface(
                    modifier = Modifier.size(84.dp),
                    shape = CircleShape,
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.background)
                ) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = stringResource(R.string.content_desc_avatar),
                        modifier = Modifier.clickable(onClick = openAvatar),
                        contentScale = ContentScale.Crop,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Bottom),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(stringResource(R.string.people_stat_voteup), user.voteupCount)
                    StatItem(stringResource(R.string.people_stat_followers), user.followerCount)
                    StatItem(stringResource(R.string.people_stat_following), user.followingCount)
                }
            }
        }

        Spacer(modifier = Modifier.height(54.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                FollowButton(
                    isFollowing = user.isFollowing == true,
                    onClick = onFollowClick,
                    modifier = Modifier.weight(1.3f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                FilledTonalButton(
                    onClick = {},
                    modifier = Modifier.weight(0.7f),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.MailOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.people_action_message))
                }
            }
        }
    }
}
