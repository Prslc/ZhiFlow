package com.prslc.zhiflow.ui.page.people.moment

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.utils.formatToDate
import com.prslc.zhiflow.data.dto.MomentDto
import com.prslc.zhiflow.data.model.moment.MediaImage
import com.prslc.zhiflow.ui.component.common.ContentMeta
import com.prslc.zhiflow.ui.component.common.ContentTypeLabel
import com.prslc.zhiflow.ui.component.common.ImageData
import com.prslc.zhiflow.ui.component.common.ThumbnailRow
import com.prslc.zhiflow.ui.navigation.LocalNavigator

@Composable
fun BaseMomentCard(
    state: MomentDto,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val navigator = LocalNavigator.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                state.routerUrl?.let { navigator.handleUrl(it) }
            }
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)
        ) {
            MomentHeader(state = state)
            content()
            Spacer(modifier = Modifier.height(20.dp))
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
private fun MomentHeader(
    state: MomentDto,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = state.authorAvatarUrl,
            contentDescription = stringResource(R.string.content_desc_avatar),
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = state.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (state.isTopping) {
                    ContentTypeLabel(text = stringResource(R.string.moment_pin))
                }
            }
            if (state.actionTime > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatToDate(state.actionTime)} · ${state.actionText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
fun UserMomentCard(
    state: MomentDto,
    modifier: Modifier = Modifier
) {
    BaseMomentCard(state = state, modifier = modifier) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = state.videoThumbnail,
                contentDescription = stringResource(R.string.content_desc_avatar),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (state.summary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun StandardMomentCard(
    state: MomentDto,
    modifier: Modifier = Modifier
) {
    BaseMomentCard(state = state, modifier = modifier) {
        if (state.title.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (state.plainContent.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.plainContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val images = state.mediaImages
        if (images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ThumbnailRow(
                images = images.mapNotNull { img ->
                    img.url?.let { ImageData(it, img.width, img.height) }
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        ContentMeta(
            voteCount = state.voteCount,
            commentCount = state.commentCount,
        )
    }
}