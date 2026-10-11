package com.prslc.zhiflow.ui.page.comment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R

/**
 * The box a comment is written in, under the list it will join.
 *
 * Sending is the caller's: the words are [draft] until it does something with them, and the box
 * only says whether one is in flight.
 *
 * @param draft What the reader has typed.
 * @param isSubmitting Whether one is on its way, which closes the button against a second.
 * @param onDraftChange Called with the box's contents on every keystroke.
 * @param onSend Called by the send button, which is only enabled on a box with something in it.
 * @param modifier Applied to the column, ahead of the padding it adds itself.
 * @param replyToName Who the words will answer, for the panes that answer someone; null for a box
 *   that starts a comment. It is drawn above the field, so that a box carried over from another
 *   pane still says whom it is addressed to.
 */
@Composable
fun CommentComposer(
    draft: String,
    isSubmitting: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    replyToName: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (replyToName != null) {
            Text(
                text = stringResource(R.string.comment_reply_to, replyToName),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            TextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text(stringResource(R.string.comment_compose_hint)) },
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                // A filled field draws a line under itself; in a box the reader writes paragraphs
                // into, that line reads as an underline on the text rather than as the field's edge.
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = draft.isNotBlank() && !isSubmitting,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    stringResource(R.string.comment_action_send),
                )
            }
        }
    }
}
