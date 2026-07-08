package com.prslc.zhiflow.ui.page.question

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.data.model.AnswerTarget
import com.prslc.zhiflow.ui.component.common.AuthorRow
import com.prslc.zhiflow.ui.component.common.ContentMeta
import com.prslc.zhiflow.ui.component.common.ImageData
import com.prslc.zhiflow.ui.component.common.ThumbnailRow

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
        Text(
            text = target.excerpt,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        // images
        val thumbnails = target.thumbnailInfo?.thumbnails ?: emptyList()
        if (thumbnails.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ThumbnailRow(
                images = thumbnails.map { ImageData(it.url, it.width, it.height) },
            )
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
