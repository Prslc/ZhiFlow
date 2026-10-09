package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.core.utils.formatCount

/**
 * One count-and-label pair of a profile header's stat row, laid out inline so a row of them reads
 * as a sentence and needs no column widths.
 *
 * Callers have to give the row a full line of its own. Beside an avatar there are only ~253dp,
 * which three inline pairs overflow once the counts reach six figures.
 *
 * @param label The name of the count, drawn after it.
 * @param count The count, drawn bold before the label. Past 10000 it is abbreviated to one decimal
 *   and a `w`; the feed's own vote line does not abbreviate.
 * @param modifier Applied to the `Row`. The pair is laid out inline, so this is the only place to
 *   give it room.
 */
@Composable
fun StatItem(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
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
