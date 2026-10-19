package com.prslc.zhiflow.ui.page.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.utils.compose.rememberToggleHaptic
import com.prslc.zhiflow.core.utils.compose.sizedImageUrl
import com.prslc.zhiflow.core.utils.formatToDate
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder
import com.prslc.zhiflow.ui.component.common.BadgeLabel
import com.prslc.zhiflow.ui.component.richtext.component.ImageComponent
import com.prslc.zhiflow.ui.component.richtext.component.rememberEmojiContent
import com.prslc.zhiflow.ui.component.richtext.withThemeSpans
import com.prslc.zhiflow.ui.component.widget.ImageLightboxController
import com.prslc.zhiflow.ui.component.widget.LightboxItem
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import com.prslc.zhiflow.ui.preview.sampleComment
import com.prslc.zhiflow.ui.preview.sampleCommentByAuthor
import com.prslc.zhiflow.ui.preview.sampleCommentByTop
import com.prslc.zhiflow.ui.preview.sampleCommentWithReplies
import com.prslc.zhiflow.ui.preview.sampleDislikedComment
import com.prslc.zhiflow.ui.preview.sampleReply
import com.prslc.zhiflow.ui.preview.sampleReplyByAuthor
import com.prslc.zhiflow.ui.theme.TextStyles
import org.koin.compose.koinInject

@Composable
fun CommentItem(
    model: CommentViewModel.CommentUiModel,
    onEvent: (CommentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    isChild: Boolean = false,
    showReplyButton: Boolean = true,
) {
    val comment = model.comment
    val toggleHaptic = rememberToggleHaptic()

    val metaStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.outline,
        fontSize = TextStyles.commentMetaSize,
    )

    val navigator = LocalNavigator.current
    val lightbox = koinInject<ImageLightboxController>()
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val body = comment.parsedContent.text.withThemeSpans()

    // The topping badge rides the comment's own first line rather than taking a line of its own;
    // `CommentToppingBadge` holds how its box is measured into that line.
    val toppingLabel = stringResource(R.string.comment_topping)
    val toppingBadge = if (comment.isAuthorTop) rememberToppingBadge(toppingLabel) else null
    val text = if (toppingBadge == null) body else body.withToppingBadge(toppingLabel)

    val inlineContent = comment.parsedContent.text.rememberEmojiContent() +
        toppingBadge?.let { mapOf(TOPPING_BADGE to it) }.orEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // avatar
        AsyncImage(
            model = sizedImageUrl(comment.author.avatarUrl, 36.dp),
            contentDescription = stringResource(R.string.content_desc_avatar),
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onEvent(CommentUiEvent.ShowAuthor(comment.author.id)) },
            contentScale = ContentScale.Crop,
        )

        // name
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onEvent(CommentUiEvent.ShowAuthor(comment.author.id)) },
                )

                if (comment.isAuthor) {
                    BadgeLabel(
                        text = stringResource(R.string.comment_author),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                comment.replyToAuthor?.let { replyTo ->
                    Text(
                        text = " ▸ ",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Text(
                        text = replyTo.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.clickable { onEvent(CommentUiEvent.ShowAuthor(replyTo.id)) },
                    )
                }
            }

            // When it was said, and from where. Under the name rather than under the body: what is
            // under the body belongs to the replies and to what can be done about them.
            comment.ipInfo?.let { ip ->
                Text(
                    text = stringResource(
                        R.string.comment_published_with_ip,
                        formatToDate(comment.createdTime),
                        ip,
                    ),
                    style = metaStyle,
                    color = MaterialTheme.colorScheme.outline,
                )
            } ?: Text(
                text = formatToDate(comment.createdTime),
                style = metaStyle,
                color = MaterialTheme.colorScheme.outline,
            )

            // comment
            // Per comment, not per list: the author row, images and timestamp between comments
            // leave nothing meaningful to select across two of them.
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    inlineContent = inlineContent,
                    onTextLayout = { layoutResult = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(comment.parsedContent.text) {
                            detectTapGestures { pos ->
                                layoutResult?.let { result ->
                                    val offset = result.getOffsetForPosition(pos)
                                    if (offset < comment.parsedContent.text.length) {
                                        comment.parsedContent.text.getStringAnnotations(AnnotatedStringBuilder.URL_TAG, offset, offset)
                                            .firstOrNull()?.let { annotation ->
                                                navigator.handleUrl(annotation.item)
                                            }
                                    }
                                }
                            }
                        },
                )
            }

            // images
            val commentImages = comment.parsedContent.images
            if (commentImages.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    commentImages.forEach { image ->
                        ImageComponent(
                            image = image,
                            onImageClick = { clicked ->
                                lightbox.open(commentImages.map { LightboxItem.Image(it) }, clicked)
                            },
                        )
                    }
                }
            }

            // What the reader can do about the comment, under it: the way in on the left, the way
            // to agree on the right. Under that come the replies, which belong to the comment the
            // way a note does -- the actions close it, and what is under them is what came of it.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Every row that takes an answer offers one, replies included. What a tap means is
                // the view model's to say: it answers the row that was tapped, in whichever pane
                // the row is drawn.
                if (comment.canReply) {
                    ReplyAction(onClick = { onEvent(CommentUiEvent.ReplyTo(comment)) })
                }

                Spacer(modifier = Modifier.weight(1f))

                if (comment.canDislike) {
                    ReactionAction(
                        active = comment.disliked,
                        icon = Icons.Outlined.ThumbDown,
                        activeIcon = Icons.Filled.ThumbDown,
                        description = if (comment.disliked) {
                            stringResource(R.string.comment_action_disagreed)
                        } else {
                            stringResource(R.string.comment_action_disagree)
                        },
                        onClick = {
                            toggleHaptic(!comment.disliked)
                            onEvent(CommentUiEvent.ToggleDislike(comment.id))
                        },
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }

                if (comment.canLike) {
                    ReactionAction(
                        active = comment.liked,
                        icon = Icons.Outlined.ThumbUp,
                        activeIcon = Icons.Filled.ThumbUp,
                        count = comment.likeCount,
                        description = if (comment.liked) {
                            stringResource(R.string.comment_action_agreed)
                        } else {
                            stringResource(R.string.comment_action_agree)
                        },
                        onClick = {
                            toggleHaptic(!comment.liked)
                            onEvent(CommentUiEvent.ToggleLike(comment.id))
                        },
                    )
                }
            }

            // The replies, where the response carried any: one of them whole, and the block itself
            // is the way into the rest. Not on a reply, and not on the comment the pane was opened
            // for -- that one is drawn above its own replies, and the way in would lead back into
            // the list it is already at the top of.
            if (!isChild && showReplyButton && comment.childCount > 0) {
                CommentReplies(
                    preview = comment.childComments.firstOrNull(),
                    count = comment.childCount,
                    onClick = { onEvent(CommentUiEvent.LoadChildComments(comment)) },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * The way to answer one comment: the box comes up addressed to it.
 */
@Composable
private fun ReplyAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.comment_action_reply),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = TextStyles.commentMetaSize,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * One of the reactions a comment wears: the thumb, and what it has been given.
 *
 * The two differ in what they draw, not in how they sit -- one weight, one colour rule, one way of
 * counting -- so they are one composable with the words and the glyph handed in.
 *
 * @param active Whether the reader's own reaction is on.
 * @param icon What it is drawn as while it is off; [activeIcon] while it is on.
 * @param description What it is called, for a reader who cannot see it.
 * @param count How many it has, where the row draws a number at all; null leaves it out.
 */
@Composable
private fun ReactionAction(
    active: Boolean,
    icon: ImageVector,
    activeIcon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (active) activeIcon else icon,
            contentDescription = description,
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(18.dp),
        )
        if (count != null && count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = TextStyles.commentLikeSize,
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
            )
        }
    }
}

/** A root comment, and a reply under it, which is indented and names who it answers. */
@AppPreviews
@Composable
private fun CommentItemPreview() {
    PreviewSurface {
        Column {
            CommentItem(
                model = CommentViewModel.CommentUiModel(sampleComment),
                onEvent = {},
            )
            CommentItem(
                model = CommentViewModel.CommentUiModel(sampleReply),
                onEvent = {},
                isChild = true,
            )
            CommentItem(
                model = CommentViewModel.CommentUiModel(sampleCommentByAuthor),
                onEvent = {},
            )
            CommentItem(
                model = CommentViewModel.CommentUiModel(sampleReplyByAuthor),
                onEvent = {},
            )
            CommentItem(
                model = CommentViewModel.CommentUiModel(sampleCommentByTop),
                onEvent = {},
            )
            CommentItem(
                model = CommentViewModel.CommentUiModel(sampleCommentWithReplies),
                onEvent = {},
            )
            CommentItem(
                model = CommentViewModel.CommentUiModel(sampleDislikedComment),
                onEvent = {},
            )
        }
    }
}
