package com.prslc.zhiflow.ui.page.comment

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface

/**
 * The box a comment is written in. It is not part of the sheet's furniture: the sheet is read
 * without one, and the box exists only once the reader has asked to write — by tapping a row's
 * reply, or by asking for a comment of their own.
 *
 * The box says whom it answers in its hint: a line above the field would sit there for as long as
 * the box is up, while the hint is what an empty field says anyway.
 *
 * Sending is the caller's: the words are [draft] until it does something with them, and the box
 * only says whether one is in flight.
 *
 * @param draft What the reader has typed.
 * @param isSubmitting Whether one is on its way, which closes the button against a second.
 * @param onDraftChange Called with the box's contents on every keystroke.
 * @param onSend Called by the send button, which is only enabled on a box with something in it.
 * @param onClose Puts the box away. A back asks for it: the reader is the one who brought the box
 *   up, so a back belongs to it rather than to the sheet it sits in.
 * @param modifier Applied to the column, ahead of the padding it adds itself.
 * @param replyToName Who the words will answer, where the reader asked to answer someone; null for
 *   a box that starts a comment.
 */
@Composable
fun CommentComposer(
    draft: String,
    isSubmitting: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    replyToName: String? = null,
) {
    // Asked for as the box comes up, because a focus no finger brought does not raise the keyboard;
    // the reader asked to write, so both are wanted.
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    BackHandler(onBack = onClose)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            TextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = {
                    Text(
                        if (replyToName != null) {
                            stringResource(R.string.comment_reply_to, replyToName)
                        } else {
                            stringResource(R.string.comment_compose_hint)
                        },
                    )
                },
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                // A filled field draws a line under itself; in a box the reader writes paragraphs
                // into, that line reads as an underline on the text rather than as the field's edge.
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .weight(1f),
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

/**
 * The way into a comment that answers no one: a FAB over the list it will join.
 *
 * It goes with the list's scrolling the way the content page's bar does — away while the list is
 * read downwards, back as soon as it is read upwards. The direction is the list state's own, so a
 * fling counts the same as a finger, and a list that cannot scroll never sets it.
 *
 * @param visible Whether it is wanted at all; the box being open is what leaves it no job.
 * @param listState The list it floats over.
 * @param onClick Opens the box.
 * @param modifier Applied to the FAB, which a caller in a `Box` anchors with its own alignment.
 */
@Composable
fun CommentComposeFab(
    visible: Boolean,
    listState: LazyListState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && !listState.lastScrolledForward,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        FloatingActionButton(onClick = onClick) {
            Icon(
                Icons.Default.Edit,
                contentDescription = stringResource(R.string.comment_action_compose),
            )
        }
    }
}

/** Writing on the content, answering someone, and the wait while it goes. */
@AppPreviews
@Composable
private fun CommentComposerPreview() {
    PreviewSurface {
        Column {
            CommentComposer(
                draft = "",
                isSubmitting = false,
                onDraftChange = {},
                onSend = {},
                onClose = {},
            )
            CommentComposer(
                draft = "The tree is only half the story.",
                isSubmitting = false,
                onDraftChange = {},
                onSend = {},
                onClose = {},
                replyToName = "Grace Hopper",
            )
            CommentComposer(
                draft = "Sending",
                isSubmitting = true,
                onDraftChange = {},
                onSend = {},
                onClose = {},
            )
        }
    }
}
