package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.dto.FeedAuthorNote
import com.prslc.zhiflow.data.dto.FeedDto
import com.prslc.zhiflow.data.dto.FeedReason
import com.prslc.zhiflow.ui.component.common.AuthorRow
import com.prslc.zhiflow.ui.component.common.ContentMeta
import com.prslc.zhiflow.ui.component.common.ContentTypeLabel
import com.prslc.zhiflow.ui.component.common.ThumbnailRow
import com.prslc.zhiflow.ui.component.common.contentTypeConfig

@Composable
fun FeedItem(
    display: FeedDto,
    onClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (String, String) -> Unit,
) {
    val stableClick = remember(display.id, display.type, onClick) {
        { onClick(display.id, display.type) }
    }
    val stableLongClick = remember(display.id, display.type, onLongClick) {
        { onLongClick(display.id, display.type) }
    }

    val typeConfig = contentTypeConfig(display.type)
    val hasTitle = display.title.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = stableClick,
                onLongClick = stableLongClick,
            )
            .padding(20.dp)
    ) {
        display.reason?.let { reason ->
            ReasonLine(reason)

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (hasTitle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContentTypeLabel(
                    text = stringResource(typeConfig.labelResId),
                    containerColor = typeConfig.containerColor,
                    contentColor = typeConfig.contentColor,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = display.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        AuthorRow(
            avatarUrl = display.authorAvatar,
            authorName = display.authorName,
            avatarSize = 20.dp,
            nameStyle = MaterialTheme.typography.labelMedium,
            nameColor = MaterialTheme.colorScheme.primary,
        ) {
            display.authorNote?.let { RelationNote(authorNoteText(it)) }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = display.excerpt,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        if (display.images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ThumbnailRow(images = display.images)
        }

        Spacer(modifier = Modifier.height(8.dp))

        ContentMeta(
            voteCount = display.voteCount,
            commentCount = display.commentCount,
        )
    }
}

@Composable
private fun ReasonLine(reason: FeedReason) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = reason.iconUrl,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline),
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = reason.text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * What a relation note says, in the reader's language where this app knows the kind.
 *
 * The server writes the sentence itself, so a kind that is not in the table -- one the server has
 * added since -- is drawn as it came. The kind that counts followers names the number only inside
 * that sentence, and it is the one kind here whose number this app reads for itself.
 *
 * @param note The chip's own text and the kind its `test_id` named.
 */
@Composable
@ReadOnlyComposable
private fun authorNoteText(note: FeedAuthorNote): String = when (note.kind) {
    "following" -> stringResource(R.string.feed_note_following)
    "FollowingMemberFollowing" -> stringResource(R.string.feed_note_following_member)
    "RelationEndorseTypeUnfollowPeopleUpvote" -> stringResource(R.string.feed_note_upvoted)
    "FollowerNum" -> note.count
        ?.let { pluralStringResource(R.plurals.feed_note_followers, it, it) }
        ?: note.text
    else -> note.text
}

@Composable
private fun RelationNote(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.padding(start = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            // The row gives the name its width first, so a long name can leave this chip short:
            // it ellipsizes rather than being clipped mid-word.
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}
