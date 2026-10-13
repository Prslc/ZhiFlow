package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.common.ColumnCover
import com.prslc.zhiflow.ui.component.richtext.bodyElementPadding
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import com.prslc.zhiflow.ui.preview.sampleColumnCard
import com.prslc.zhiflow.ui.theme.TextStyles

/**
 * The card a body ends with when it was published in a column: the column it belongs to, and a way
 * into it.
 *
 * It is drawn as the other cards are — one surface, no text to select, the whole of it the tap
 * target — but what it says is the server's own: the line over it, the column's name and counts,
 * and what the column last published.
 *
 * @param element The card segment. Its token is the column a tap opens, through the navigator.
 * @param modifier Applied to the surface, ahead of the padding and the 4dp of vertical room it
 *   adds itself.
 */
@Composable
fun ColumnCardComponent(
    element: RichTextElement.ColumnCard,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current

    DisableSelection {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { navigator.navigateToColumn(element.columnToken) },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (element.headerTips.isNotBlank()) {
                    Text(
                        text = element.headerTips,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColumnCover(
                        url = element.cover,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = element.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = TextStyles.cardTitleBold,
                                lineHeight = TextStyles.cardTitleLineHeight,
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (element.desc.isNotBlank()) {
                            Text(
                                text = element.desc,
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = TextStyles.cardDescSize,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (element.latestText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = element.latestText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@AppPreviews
@Composable
private fun ColumnCardComponentPreview() {
    PreviewSurface {
        ColumnCardComponent(
            element = sampleColumnCard,
            modifier = Modifier.padding(bodyElementPadding(sampleColumnCard)),
        )
    }
}

/** The same card for a column the server sent no picture for. */
@AppPreviews
@Composable
private fun ColumnCardComponentNoCoverPreview() {
    val element = sampleColumnCard.copy(cover = null)
    PreviewSurface {
        ColumnCardComponent(
            element = element,
            modifier = Modifier.padding(bodyElementPadding(element)),
        )
    }
}
