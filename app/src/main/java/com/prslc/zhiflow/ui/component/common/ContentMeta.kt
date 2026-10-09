package com.prslc.zhiflow.ui.component.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import com.prslc.zhiflow.R

/**
 * The votes and comments of one card, as the single line the lists show them on.
 *
 * @param voteCount The votes as the API reported them, in full: this line does not abbreviate, so
 *   12000 reads as 12000 where the profile's stat row would say 1.2w.
 * @param commentCount The comments as the API reported them; a card with none shows 0.
 * @param modifier Applied to the `Text`, so a caller can place the line inside its own layout.
 */
@Composable
fun ContentMeta(
    voteCount: Int,
    commentCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = pluralStringResource(R.plurals.feed_meta, commentCount, voteCount, commentCount),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier,
    )
}
