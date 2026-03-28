package com.prslc.zhiflow.ui.component.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prslc.zhiflow.data.model.ZhihuComment
import com.prslc.zhiflow.parser.ContentParser
import com.prslc.zhiflow.ui.component.ImageComponent
import com.prslc.zhiflow.utils.formatToDate

@Composable
fun CommentItem(
    comment: ZhihuComment,
    modifier: Modifier = Modifier,
    onAuthorClick: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val parsedContent = remember(comment.content) {
        ContentParser.parseCommentHtml(comment.content)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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


        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // name
            Text(
                text = comment.author.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.clickable { onAuthorClick(comment.author.id) }
            )

            // comment
            Text(
                text = parsedContent.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // IP and time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                comment.tags.find { it.type == "ip_info" }?.text?.let { ip ->
                    Text(
                        text = ip,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Text(
                    text = formatToDate(comment.createdTime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
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