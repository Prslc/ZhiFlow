package com.prslc.zhiflow.ui.component

import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import coil.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.FeedItem

@Composable
fun ZhihuFeedItem(
    item: FeedItem,
    onClick: (String, String) -> Unit   // id, type
) {
    val target = item.target ?: return
    val type = target.type ?: "answer"
    val title = target.question?.title ?: target.title ?: stringResource(R.string.unknown_content)

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = {
                    val id = target.id?.toString()
                    if (id != null) {
                        onClick(id, type)
                    }
                },
                onLongClick = {
                    target.id?.let { id ->
                        clipboardManager.setText(AnnotatedString(id.toString()))

                        Toast.makeText(context, "ID has been copied to the clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // title
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeLabel(target.type)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // avatar
                AsyncImage(
                    model = target.author?.avatarUrl,
                    contentDescription = stringResource(R.string.avatar_desc),
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    contentScale = Crop
                )

                Spacer(modifier = Modifier.width(8.dp))

                // author
                Text(
                    text = target.author?.name ?: stringResource(R.string.anonymous_user),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Content
            Text(
                text = target.excerpt ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // bottom
            Text(
                text = stringResource(
                    R.string.feed_meta,
                    target.voteCount,
                    target.commentCount
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun TypeLabel(type: String?) {
    val (label, containerColor, contentColor) = when (type) {
        "answer" -> Triple(
            stringResource(R.string.type_answer),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary
        )

        "article" -> Triple(
            stringResource(R.string.type_article),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.tertiary
        )

        else -> Triple(
            stringResource(R.string.type_unknown),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold
        )
    }
}