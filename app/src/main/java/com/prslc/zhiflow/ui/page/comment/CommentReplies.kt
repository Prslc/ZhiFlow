package com.prslc.zhiflow.ui.page.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.dto.CommentDto
import com.prslc.zhiflow.ui.component.richtext.withThemeSpans

/**
 * The replies a comment has, in the comment's own card: one of them whole, and the way into the
 * rest.
 *
 * A count on its own said that a conversation was going on without saying anything about it, and
 * what has been said back is the thing a reader decides by. The reply is drawn as the body is,
 * marks and all, and the whole block is what opens the pane the rest of them are read in.
 *
 * @param preview The reply the response carried. Null where it carried none -- a count is then all
 *   there is to show, and the block still opens the pane.
 * @param count How many replies the comment has, which is what the second line counts.
 * @param onClick What opens them.
 * @param modifier Applied to the block, so a caller's spacing lands outside its own.
 */
@Composable
internal fun CommentReplies(
    preview: CommentDto?,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        preview?.let { reply ->
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(reply.author.name)
                    }
                    append(": ")
                    append(reply.parsedContent.text.withThemeSpans())
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = pluralStringResource(R.plurals.comment_reply_count_with_arrow, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
