package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.ui.component.widget.rememberSingleImageLightbox

private val CoverHeight = 160.dp
private val AvatarSize = 84.dp
private val HeaderGutter = 20.dp

/** Half the 84dp avatar, so it sits centred on the cover's bottom edge. */
private val AvatarOverhang = 42.dp

/**
 * Where the name starts, measured from the cover's bottom edge. Clears [AvatarOverhang] by the 12dp
 * the design has always left between the avatar and the name; the two drift apart the moment they
 * are separate literals.
 */
private val HeaderTextTop = AvatarOverhang + 12.dp

/**
 * How far the action row drops below the avatar's bottom edge. Material pads a button's touch
 * target 4dp past its visual box on every side, so bottom-aligning the box alone would leave the
 * pills 4dp high; the other 8dp is optical. This is the lever for "the actions sit too high" --
 * moving [AvatarOverhang] would drag the avatar down with them.
 */
private val ActionOverhang = 12.dp

/** One count-and-label pair of the header's stat row. */
@Immutable
data class ProfileStat(val label: String, val count: Int)

/**
 * The header both profile pages share: a full-bleed cover with the avatar hanging over its bottom
 * edge on the left and the page's actions in the same row on the right, then the name, headline and
 * stat row below.
 *
 * The cover sits behind the status bar, so [StatusBarScrim] darkens that strip; anything a caller
 * puts on the cover itself has to carry contrast of its own -- see [iconScrim].
 *
 * @param user The profile being shown: its cover, avatar, name and headline are all read off it.
 * @param stats Counts for the row under the headline. Keep it short: the row is a single line and
 * three pairs already need ~330dp of the 353dp available.
 * @param modifier Applied to the header's `Box`, so the caller sizes the whole cover block.
 * @param coverContentDescription Null when the cover is decorative rather than a thing to open.
 * @param topEndAction A control for the cover's top-right corner. The slot is positioned and given
 * an [iconScrim] disc and white content colour; the caller supplies only the icon button.
 * @param actions The right-hand side of the avatar row. Already offset by [ActionOverhang]; do not
 * add the offset again.
 */
@Composable
fun ProfileCoverHeader(
    user: ZhihuUser,
    stats: List<ProfileStat>,
    modifier: Modifier = Modifier,
    coverContentDescription: String? = null,
    topEndAction: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    val openCover = rememberSingleImageLightbox(user.coverUrl)
    val openAvatar = rememberSingleImageLightbox(user.avatar)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = user.coverUrl,
                contentDescription = coverContentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CoverHeight)
                    .clickable(onClick = openCover),
                contentScale = ContentScale.Crop,
            )

            StatusBarScrim()

            if (topEndAction != null) {
                val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = statusBarHeight + 4.dp, end = 8.dp)
                        .iconScrim(0.55f),
                ) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        topEndAction()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = HeaderGutter)
                    .offset(y = AvatarOverhang),
                verticalAlignment = Alignment.Bottom,
            ) {
                Surface(
                    modifier = Modifier.size(AvatarSize),
                    shape = CircleShape,
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
                ) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = stringResource(R.string.content_desc_avatar),
                        modifier = Modifier.clickable(onClick = openAvatar),
                        contentScale = ContentScale.Crop,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.offset(y = ActionOverhang),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }

        Spacer(modifier = Modifier.height(HeaderTextTop))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HeaderGutter)
        ) {
            Text(
                text = user.name ?: stringResource(R.string.profile_default_username),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = user.headline ?: stringResource(R.string.profile_default_headline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                stats.forEach { StatItem(it.label, it.count) }
            }
        }
    }
}
