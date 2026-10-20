package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface

/**
 * How much there is to read about a question, as the one line both pages that show it draw.
 *
 * The question page draws it under its topics, before the answers it counts; the answer page draws
 * it under the question's title, which is that page's own title. The answer count leads, in the
 * color a way in is drawn in, because it is where the answers are; the follower count follows in
 * the quieter color of a fact about the line rather than an invitation.
 *
 * @param answerCount The answers as the API reported them, in full: this line does not abbreviate,
 *   so 12000 reads as 12000 where the profile's stat row would say 1.2w.
 * @param followerCount The followers, numbered the same way.
 * @param modifier Applied to the row, so a page can place it inside its own padding.
 */
@Composable
fun QuestionStats(
    answerCount: Int,
    followerCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = pluralStringResource(
                R.plurals.question_stats_answers,
                answerCount,
                answerCount,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = " · ",
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = pluralStringResource(
                R.plurals.question_stats_followers,
                followerCount,
                followerCount,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@AppPreviews
@Composable
private fun QuestionStatsPreview() {
    PreviewSurface {
        QuestionStats(
            answerCount = 1284,
            followerCount = 4200,
            modifier = Modifier.padding(20.dp),
        )
    }
}
