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
import com.prslc.zhiflow.data.model.FeedItem
import com.prslc.zhiflow.ui.component.common.AuthorRow
import com.prslc.zhiflow.ui.component.common.ContentMeta
import com.prslc.zhiflow.ui.component.common.ContentTypeLabel
import com.prslc.zhiflow.ui.component.common.contentTypeConfig

@Composable
fun FeedItem(
    item: FeedItem,
    modifier: Modifier = Modifier,
    onClick: (String, String) -> Unit,   // id, type
) {
    val target = item.target ?: return

    val (type, title) = remember(item) {
        val type = target.type ?: "answer"
        val title = target.question?.title ?: target.title ?: ""
        type to title
    }

    val stableClick = remember(target.id, type, onClick) {
        {
            val id = target.id?.toString()
            if (id != null) {
                onClick(id, type)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = stableClick)
            .padding(20.dp)
    ) {
        // title
        Row(verticalAlignment = Alignment.CenterVertically) {
            val config = contentTypeConfig(target.type)
            ContentTypeLabel(
                text = stringResource(config.labelResId),
                containerColor = config.containerColor,
                contentColor = config.contentColor,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AuthorRow(
            avatarUrl = target.author?.avatarUrl,
            authorName = target.author?.name ?: "",
            avatarSize = 20.dp,
            nameStyle = MaterialTheme.typography.labelMedium,
            nameColor = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Content
        Text(
            text = target.excerpt ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        ContentMeta(
            voteCount = target.voteCount,
            commentCount = target.commentCount,
        )
    }
}
