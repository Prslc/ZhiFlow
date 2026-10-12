package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.core.utils.compose.sizedImageUrl
import com.prslc.zhiflow.data.dto.PersonDto
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import com.prslc.zhiflow.ui.preview.sampleFollowedPerson
import com.prslc.zhiflow.ui.preview.samplePerson

/**
 * One person in a list of them: their picture, their name, and the line they introduce themselves
 * with.
 *
 * The row is a single line of text beside a 44dp avatar, and the name and the headline are each
 * cut at one line — a long headline is a teaser here, not the person's page.
 *
 * @param person Who to draw.
 * @param onClick Called when the row is tapped.
 * @param modifier Applied to the `Row`, ahead of the padding this adds itself.
 */
@Composable
fun PersonRow(
    person: PersonDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = sizedImageUrl(person.avatarUrl, 44.dp),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (person.headline.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = person.headline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One already followed, one not: the row itself does not draw the difference. */
@AppPreviews
@Composable
private fun PersonRowPreview() {
    PreviewSurface {
        Column {
            PersonRow(person = samplePerson, onClick = {})
            PersonRow(person = sampleFollowedPerson, onClick = {})
        }
    }
}
