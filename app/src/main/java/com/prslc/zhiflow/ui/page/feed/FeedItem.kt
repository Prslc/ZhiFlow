package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.data.dto.FeedDto
import com.prslc.zhiflow.ui.component.common.AuthorRow
import com.prslc.zhiflow.ui.component.common.ContentMeta
import com.prslc.zhiflow.ui.component.common.ContentTypeLabel
import com.prslc.zhiflow.ui.component.common.ThumbnailRow
import com.prslc.zhiflow.ui.component.common.contentTypeConfig

@Composable
fun FeedItem(
    display: FeedDto,
    modifier: Modifier = Modifier,
    onClick: (String, String) -> Unit,
) {
    val stableClick = remember(display.id, display.type, onClick) {
        { onClick(display.id, display.type) }
    }

    val typeConfig = contentTypeConfig(display.type)
    val hasTitle = display.title.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = stableClick)
            .padding(20.dp)
    ) {
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
        )

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
