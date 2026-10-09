package com.prslc.zhiflow.ui.page.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.dto.FeedbackAction

/**
 * The wording one panel row wears, or the server's own where this app does not know the row.
 *
 * The panel is server-driven: which rows appear and what they say arrive together, in the server's
 * language. A row is therefore looked up by its `module_id` and drawn in the reader's language,
 * and a module the table has never seen keeps the sentence that came with it -- which is what
 * every row did before there was a table.
 *
 * One row takes a word from the content it was opened on: the author's name, which the server
 * folds into its own sentence. The remaining composed row, the one about topics, wears the wording
 * the block page wears -- the same fallback the server itself takes when it has no topics to name.
 *
 * @param action The row to name.
 * @param authorName The author of the card the panel was opened on, where the feed still holds it.
 */
@Composable
@ReadOnlyComposable
fun feedbackRowLabel(action: FeedbackAction, authorName: String?): String = when (action.moduleId) {
    "ignore_reduce_similar" -> stringResource(R.string.feedback_row_not_interested)
    "ignore_author" -> authorName?.let { stringResource(R.string.feedback_row_author, it) }
        ?: action.label
    "ignore_story" -> stringResource(R.string.feedback_row_story)
    "ignore_keyword" -> stringResource(R.string.feedback_row_keywords)
    "ignore_outdated" -> stringResource(R.string.feedback_row_repetitive)
    "ignore_unfriendly" -> stringResource(R.string.feedback_row_extreme)
    "ignore_vulgar" -> stringResource(R.string.feedback_row_vulgar)
    "ignore_poor_content" -> stringResource(R.string.feedback_row_poor)
    else -> action.label
}

/**
 * The line a row reports once its request has gone through, or null where it reports nothing.
 *
 * @param action The row that was taken. Only a row that runs a request has a line; one that opens
 *   a page reports by going there.
 */
@Composable
@ReadOnlyComposable
fun feedbackToastText(action: FeedbackAction.Request): String? = when (action.moduleId) {
    "ignore_reduce_similar" -> stringResource(R.string.feedback_toast_not_interested)
    "ignore_author" -> stringResource(R.string.feedback_toast_author)
    "ignore_story" -> stringResource(R.string.feedback_toast_story)
    "ignore_outdated" -> stringResource(R.string.feedback_toast_repetitive)
    "ignore_unfriendly" -> stringResource(R.string.feedback_toast_extreme)
    "ignore_vulgar" -> stringResource(R.string.feedback_toast_vulgar)
    "ignore_poor_content" -> stringResource(R.string.feedback_toast_poor)
    else -> action.toastText
}
