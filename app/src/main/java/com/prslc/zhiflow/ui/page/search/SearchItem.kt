package com.prslc.zhiflow.ui.page.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.data.dto.SearchItemDto
import com.prslc.zhiflow.data.dto.SuggestedQueryDto
import com.prslc.zhiflow.ui.component.common.AuthorRow
import com.prslc.zhiflow.ui.component.common.ContentMeta
import com.prslc.zhiflow.ui.component.common.ThumbnailRow

/**
 * One result in a page of results.
 *
 * Both the title and the excerpt arrive with the words the query matched already marked, which is
 * why they are drawn as they come rather than being wrapped in anything.
 *
 * @param item The result to draw.
 * @param onClick Called with the content's id and type when the row is tapped.
 * @param modifier Applied to the column.
 */
@Composable
internal fun SearchResultCard(
    item: SearchItemDto.Content,
    onClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(item.id, item.type) }
            .padding(20.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        AuthorRow(
            avatarUrl = item.authorAvatar,
            authorName = item.authorName,
            modifier = Modifier.padding(top = 6.dp),
            avatarSize = 20.dp,
            nameStyle = MaterialTheme.typography.labelMedium,
            nameColor = MaterialTheme.colorScheme.primary,
        )

        if (item.excerpt.isNotBlank()) {
            Text(
                text = item.excerpt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (item.images.isNotEmpty()) {
            ThumbnailRow(
                images = item.images,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        ContentMeta(
            voteCount = item.voteCount,
            commentCount = item.commentCount,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * The narrower searches for the phrase whose results are on screen, pinned under the box.
 *
 * Each chip reads the ending the server sent and searches the whole phrase it completes: the
 * ending on its own would look for something else. A row that scrolls sideways rather than a list,
 * for the reason the related queries below are one.
 *
 * @param refinements The narrower searches to draw.
 * @param onClick Called with the phrase that was tapped, which is itself a search.
 * @param modifier Applied to the scrolling row.
 */
@Composable
internal fun RefinementRow(
    refinements: List<SuggestedQueryDto>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        refinements.forEach { refinement ->
            SuggestionChip(
                onClick = { onClick(refinement.query) },
                label = {
                    Text(
                        text = refinement.suffix,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

/**
 * The narrower phrases the server sends along with a page of results.
 *
 * A row that scrolls sideways rather than a list: there are a dozen of them, they are all the
 * same kind of thing, and none of them is what the reader came for.
 *
 * @param item The phrases to draw.
 * @param onClick Called with the phrase that was tapped, which is itself a search.
 * @param modifier Applied to the scrolling row.
 */
@Composable
internal fun RelatedQueriesRow(
    item: SearchItemDto.RelatedQueries,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item.queries.forEach { phrase ->
            SuggestionChip(
                onClick = { onClick(phrase) },
                label = {
                    Text(
                        text = phrase,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
