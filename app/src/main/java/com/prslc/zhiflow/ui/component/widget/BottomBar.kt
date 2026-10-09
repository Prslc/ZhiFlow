package com.prslc.zhiflow.ui.component.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.utils.compose.rememberToggleHaptic
import com.prslc.zhiflow.core.utils.formatCount

/**
 * The action bar a content page pins to the bottom: vote, favourite, comment.
 *
 * The two vote directions arrive as separate flags rather than as one value, so a caller seeding
 * the bar from the API passes both as the API gave them.
 *
 * @param isUpvoted Whether the reader's upvote is on. The button's colours animate with it.
 * @param isDownvoted Whether the reader's downvote is on; it tints the icon in the error colour.
 * @param isFavorite Whether the content is in the reader's collection.
 * @param upvoteCount The votes to show, or null to draw the button with no count at all.
 *   Abbreviated past 10000, like the profile's stat row.
 * @param favCount The collections to show, or null to leave the label empty.
 * @param commentCount The comments to show, or null to leave the label empty.
 * @param onVoteClick Called with `"up"` or `"down"`, with the haptic already played. The bar holds
 *   no vote state, so what a second tap on the same direction means is the caller's.
 * @param onStarClick Called when the favourite button is pressed.
 * @param modifier Applied to the bar's `Surface`, ahead of the `fillMaxWidth` it adds itself.
 * @param onCommentClick Called when the comment button is pressed.
 */
@Composable
fun BottomBar(
    isUpvoted: Boolean,
    isDownvoted: Boolean,
    isFavorite: Boolean,
    upvoteCount: Int?,
    favCount: Int?,
    commentCount: Int?,
    onVoteClick: (String) -> Unit,
    onStarClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCommentClick: () -> Unit
) {
    val upvoteBgColor by animateColorAsState(
        targetValue = if (isUpvoted) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "upvoteBg",
    )
    val upvoteContentColor by animateColorAsState(
        targetValue = if (isUpvoted) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "upvoteContent",
    )

    val toggleHaptic = rememberToggleHaptic()

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(upvoteBgColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upvote
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            toggleHaptic(!isUpvoted)
                            onVoteClick("up")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isUpvoted) Icons.Filled.ArrowUpward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = upvoteContentColor,
                            modifier = Modifier.size(18.dp),
                        )
                        if (upvoteCount != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.bottom_upvote, formatCount(upvoteCount)
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = upvoteContentColor,
                            )
                        }
                    }
                }

                VerticalDivider(
                    modifier = Modifier.height(18.dp),
                    thickness = 1.dp,
                    color = upvoteContentColor.copy(alpha = 0.15f),
                )

                // Downvote
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight()
                        .clickable {
                            toggleHaptic(!isDownvoted)
                            onVoteClick("down")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isDownvoted) MaterialTheme.colorScheme.error else upvoteContentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // save and comment
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomActionItem(
                    icon = if (isFavorite) Icons.Filled.Star else Icons.Default.Star,
                    label = favCount?.let(::formatCount).orEmpty(),
                    iconTint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onStarClick,
                )
                BottomActionItem(
                    icon = Icons.AutoMirrored.Default.Comment,
                    label = commentCount?.let(::formatCount).orEmpty(),
                    onClick = onCommentClick,
                )
            }
        }
    }
}

@Composable
private fun BottomActionItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    // fillMaxHeight keeps the icon at a fixed offset whether or not the label is
    // rendered, so a count arriving never nudges the icon.
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
