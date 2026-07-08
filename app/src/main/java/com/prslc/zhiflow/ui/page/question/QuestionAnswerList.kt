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
import com.prslc.zhiflow.data.mapper.AnswerDisplay
import com.prslc.zhiflow.ui.component.common.AuthorRow
import com.prslc.zhiflow.ui.component.common.ContentMeta
import com.prslc.zhiflow.ui.component.common.ThumbnailRow

@Composable
fun AnswerItem(
    display: AnswerDisplay,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(display.id) }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        AuthorRow(
            avatarUrl = display.authorAvatar,
            authorName = display.authorName,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // content
        Text(
            text = display.excerpt,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        // images
        if (display.images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ThumbnailRow(images = display.images)
        }

        // status
        ContentMeta(
            voteCount = display.voteCount,
            commentCount = display.commentCount,
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
