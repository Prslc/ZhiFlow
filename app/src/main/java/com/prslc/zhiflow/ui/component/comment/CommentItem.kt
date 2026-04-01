package com.prslc.zhiflow.ui.component.comment

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.ZhihuComment
import com.prslc.zhiflow.parser.ContentParser
import com.prslc.zhiflow.ui.component.richtext.ImageComponent
import com.prslc.zhiflow.utils.formatToDate

@Composable
fun CommentItem(
    comment: ZhihuComment,
    modifier: Modifier = Modifier,
    isChild: Boolean = false,
    showReplyButton: Boolean = true,
    onAuthorClick: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onShowReplies: (ZhihuComment) -> Unit = {}
) {
    val parsedContent = remember(comment.content) {
        ContentParser.parseCommentHtml(comment.content)
    }

    // emoji
    val inlineContent = remember(parsedContent.text) {
        val map = mutableMapOf<String, InlineTextContent>()
        val text = parsedContent.text

        text.getStringAnnotations("EMOJI_PATH", 0, text.length).forEach { pathAnno ->
            val idAnno = text.getStringAnnotations("EMOJI_ID", pathAnno.start, pathAnno.end).firstOrNull()

            if (idAnno != null) {
                val inlineId = idAnno.item
                map[inlineId] = InlineTextContent(
                    placeholder = Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.Center),
                    children = {
                        AsyncImage(
                            model = pathAnno.item,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
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
                .clickable { onAuthorClick(comment.author.id) },
            contentScale = ContentScale.Crop
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
                    modifier = Modifier.clickable { onAuthorClick(comment.author.id) }
                )

                comment.replyToAuthor?.let { replyTo ->
                    Text(
                        text = " ▸ ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = replyTo.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable { onAuthorClick(replyTo.id) }
                    )
                }
            }

            // comment
            Text(
                text = parsedContent.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                inlineContent = inlineContent
            )

            // images
            if (parsedContent.images.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parsedContent.images.forEach { zhihuImage ->
                        ImageComponent(
                            image = zhihuImage,
                            onImageClick = onImageClick
                        )
                    }
                }
            }

            // IP
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = formatToDate(comment.createdTime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                comment.tags.find { it.type == "ip_info" }?.text?.let { ip ->
                    Text(
                        text = ip,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (!isChild && comment.childCount > 0 && showReplyButton) {
                    Text(
                        text = stringResource(R.string.comment_reply_count_with_arrow, comment.childCount),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onShowReplies(comment) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Upvote
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { onLikeClick(comment.id) }
        ) {
            Icon(
                imageVector = if (comment.liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = "like",
                tint = if (comment.liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
            if (comment.likeCount > 0) {
                Text(
                    text = comment.likeCount.toString(),
                    fontSize = 11.sp,
                    color = if (comment.liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}