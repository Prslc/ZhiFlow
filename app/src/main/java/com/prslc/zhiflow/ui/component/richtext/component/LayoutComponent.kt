package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.richtext.ZRichText
import com.prslc.zhiflow.ui.theme.TextStyles

/**
 * The rule a body draws between two of its blocks.
 *
 * @param modifier Applied to the divider ahead of the 12dp of vertical padding it adds.
 */
@Composable
fun Divider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

/**
 * A heading of a body.
 *
 * @param element The heading segment. Its level picks the text style, so two headings at the same
 *   level look the same however deep in the document they sit.
 * @param modifier Applied to the text ahead of the 8dp of top padding it adds.
 */
@Composable
fun Heading(
    element: RichTextElement.Heading,
    modifier: Modifier = Modifier
) {
    ZRichText(
        content = element.content,
        inlineMetas = element.inlineMetas,
        style = TextStyles.headingStyle(element.level),
        modifier = modifier.padding(top = 8.dp),
    )
}

/**
 * A quoted passage, with the rule that runs down its left side.
 *
 * The rule is drawn to the height of the quote rather than to a size of its own, which is why the
 * row is measured with an intrinsic height.
 *
 * @param element The quoted segment.
 * @param modifier Applied to the row ahead of its 8dp of vertical padding.
 */
@Composable
fun BlockquoteComponent(
    element: RichTextElement.Blockquote,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(IntrinsicSize.Min)
    ) {
        Canvas(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .padding(vertical = 2.dp)
        ) {
            drawRoundRect(
                color = Color.LightGray.copy(alpha = 0.6f),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }

        ZRichText(
            content = element.content,
            inlineMetas = element.inlineMetas,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 24.sp
            ),
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/**
 * A table of a body.
 *
 * Its cells are a fixed width rather than the page's, so a table wider than the screen scrolls
 * sideways. A formula inside a cell is clamped to that cell for the same reason: against the page's
 * width it would be drawn across the cells beside it. Selection is off inside the table, where a
 * drag would fight the sideways scroll.
 *
 * @param element The table segment.
 * @param modifier Applied to the box that centres the table, ahead of the `fillMaxWidth` it adds.
 */
@Composable
fun TableComponent(
    element: RichTextElement.Table,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val cellWidth = 120.dp
    val cellPadding = 8.dp
    val cellContentWidthDp = (cellWidth - cellPadding * 2).value

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Fixed-width cells inside a horizontal scroll; a selection dragged across them would fight
        // it.
        DisableSelection {
            Card(
                colors = CardDefaults.cardColors(
                    // The grid draws its own edges; a filled container under them only muddies the
                    // band and the lines. The content colour has to be stated, since a transparent
                    // container maps to none.
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.wrapContentWidth(Alignment.Start),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            ) {
                Box(modifier = Modifier.horizontalScroll(scrollState)) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        for (rowIndex in 0 until element.rows) {
                            val isHeader = rowIndex == 0 && element.hasHeader

                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isHeader) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                                .copy(alpha = 0.4f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (colIndex in 0 until element.cols) {
                                    val cell = element.cells.getOrNull(rowIndex * element.cols + colIndex)

                                    Box(
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .fillMaxHeight()
                                            .padding(cellPadding),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        cell?.let { nonNullCell ->
                                            ZRichText(
                                                content = nonNullCell.content,
                                                inlineMetas = nonNullCell.inlineMetas,
                                                maxFormulaWidthDp = cellContentWidthDp,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            )
                                        }
                                    }

                                    if (colIndex < element.cols - 1) {
                                        VerticalDivider(
                                            modifier = Modifier.fillMaxHeight(),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                                .copy(alpha = 0.5f),
                                        )
                                    }
                                }
                            }

                            if (rowIndex < element.rows - 1) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                        .copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
