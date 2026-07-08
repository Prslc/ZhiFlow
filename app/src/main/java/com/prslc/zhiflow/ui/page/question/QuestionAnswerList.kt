package com.prslc.zhiflow.ui.page.question

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.data.model.AnswerTarget
import com.prslc.zhiflow.ui.component.common.AuthorRow
import com.prslc.zhiflow.ui.component.common.ContentMeta

@Composable
fun AnswerItem(
    target: AnswerTarget,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(target.id) }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        AuthorRow(
            avatarUrl = target.author.avatar,
            authorName = target.author.name ?: "",
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // content
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = target.excerpt,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Image
            val thumbnailUrl = target.thumbnailInfo?.thumbnails?.firstOrNull()?.url
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 104.dp, height = 68.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // status
        ContentMeta(
            voteCount = target.voteupCount,
            commentCount = target.commentCount,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
fun AnswerDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
