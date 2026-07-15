package com.prslc.zhiflow.ui.page.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.utils.formatToDate
import com.prslc.zhiflow.ui.component.richtext.component.ImageComponent
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.theme.TextStyles

@Composable
fun CommentItem(
    model: CommentViewModel.CommentUiModel,
    onEvent: (CommentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    isChild: Boolean = false,
    showReplyButton: Boolean = true
) {
    val comment = model.comment

    val metaStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.outline,
        fontSize = TextStyles.commentMetaSize,
    )

    val navigator = LocalNavigator.current
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // emoji
    val inlineContent = remember(comment.parsedContent.text) {
        val map = mutableMapOf<String, InlineTextContent>()
        val text = comment.parsedContent.text

        text.getStringAnnotations("EMOJI_PATH", 0, text.length).forEach { pathAnno ->
            val idAnno =
                text.getStringAnnotations("EMOJI_ID", pathAnno.start, pathAnno.end).firstOrNull()

            if (idAnno != null) {
                val inlineId = idAnno.item
                map[inlineId] = InlineTextContent(
                    placeholder = Placeholder(
                        width = TextStyles.emojiSize,
                        height = TextStyles.emojiSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    ),
                    children = {
                        AsyncImage(
                            model = pathAnno.item,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                )
            }
        }
        map
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // avatar
        AsyncImage(
            model = comment.author.avatarUrl,
            contentDescription = "avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onEvent(CommentUiEvent.ShowAuthor(comment.author.id)) },
            contentScale = ContentScale.Crop,
        )

        // name
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onEvent(CommentUiEvent.ShowAuthor(comment.author.id)) },
                )

                comment.replyToAuthor?.let { replyTo ->
                    Text(
                        text = " ▸ ",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Text(
                        text = replyTo.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable { onEvent(CommentUiEvent.ShowAuthor(replyTo.id)) },
                    )
                }
            }

            // comment
            Text(
                text = comment.parsedContent.text,
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
                                    comment.parsedContent.text.getStringAnnotations("URL", offset, offset)
                                        .firstOrNull()?.let { annotation ->
                                            navigator.handleUrl(annotation.item)
                                        }
                                }
                            }
                        }
                    }
            )

            // images
            if (comment.parsedContent.images.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comment.parsedContent.images.forEach { image ->
                        ImageComponent(
                            image = image,
                            onImageClick = { url -> onEvent(CommentUiEvent.OpenImage(url)) },
                        )
                    }
                }
            }

            // Create Time + IP
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                comment.ipInfo?.let { ip ->
                    Text(
                        text = stringResource(
                            R.string.comment_published_with_ip,
                            formatToDate(comment.createdTime),
                            ip
                        ),
                        style = metaStyle,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } ?: Text(
                    text = formatToDate(comment.createdTime),
                    style = metaStyle,
                    color = MaterialTheme.colorScheme.outline,
                )

                // Sub-comment
                if (!isChild && comment.childCount > 0 && showReplyButton) {
                    Text(
                        text = stringResource(
                            R.string.comment_reply_count_with_arrow,
                            comment.childCount
                        ),
                        style = metaStyle.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .clickable { onEvent(CommentUiEvent.LoadChildComments(comment)) }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }

        // Upvote
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { onEvent(CommentUiEvent.ToggleLike(comment.id)) }
        ) {
            Icon(
                imageVector = if (comment.liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = "like",
                tint = if (comment.liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
            if (comment.likeCount > 0) {
                Text(
                    text = comment.likeCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = TextStyles.commentLikeSize,
                        color = if (comment.liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                )
            }
        }
    }
}
