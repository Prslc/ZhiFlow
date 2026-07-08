package com.prslc.zhiflow.ui.component.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.prslc.zhiflow.R

@Composable
fun ContentMeta(
    voteCount: Int,
    commentCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.feed_meta, voteCount, commentCount),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier,
    )
}
