package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.core.utils.formatCount
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface

/**
 * One count-and-label pair of a profile header's stat row, laid out inline so a row of them reads
 * as a sentence and needs no column widths.
 *
 * Callers have to give the row a full line of its own. Beside an avatar there are only ~253dp,
 * which three inline pairs overflow once the counts reach six figures.
 *
 * @param label The name of the count, drawn after it.
 * @param count The count, drawn bold before the label. It is written as [formatCount] writes a
 *   large one; the feed's own vote line does not abbreviate.
 * @param modifier Applied to the `Row`. The pair is laid out inline, so this is the only place to
 *   give it room.
 * @param onClick Called when the pair is tapped, for a count that leads somewhere. The pair is
 *   text on a line, so the target is no taller than the text; null leaves it inert.
 */
@Composable
fun StatItem(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** The size the abbreviation starts to matter at, beside the one that reads as a number. */
@AppPreviews
@Composable
private fun StatItemPreview() {
    PreviewSurface {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatItem(label = "Answers", count = 128)
            StatItem(label = "Followers", count = 12400)
            StatItem(label = "Agreements", count = 98211)
        }
    }
}
