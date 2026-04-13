package com.prslc.zhiflow.ui.component.richtext

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
import androidx.compose.material3.Card
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
import com.prslc.zhiflow.parser.RichTextElement

@Composable
fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
fun Heading(element: RichTextElement.Heading) {
    ZRichText(
        content = element.content,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (element.level == 3) 20.sp else 22.sp
        ),
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun BlockquoteComponent(element: RichTextElement.Blockquote) {
    Row(
        modifier = Modifier
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
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }

        ZRichText(
            content = element.content,
            inlineMetas = element.inlineMetas,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 24.sp
            ),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun TableComponent(element: RichTextElement.Table) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.wrapContentWidth(Alignment.Start),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(modifier = Modifier.horizontalScroll(scrollState)) {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    for (rowIndex in 0 until element.rows) {
                        val isHeader = rowIndex == 0 && element.hasHeader

                        Row(
                            modifier = Modifier
                                .background(
                                    if (isHeader) MaterialTheme.colorScheme.surfaceVariant
                                    else Color.Transparent
                                )
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (colIndex in 0 until element.cols) {
                                val cell = element.cells.getOrNull(rowIndex * element.cols + colIndex)

                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .fillMaxHeight()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    cell?.let { nonNullCell ->
                                        ZRichText(
                                            content = nonNullCell.content,
                                            inlineMetas = nonNullCell.inlineMetas,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }

                                if (colIndex < element.cols - 1) {
                                    VerticalDivider(modifier = Modifier.fillMaxHeight())
                                }
                            }
                        }

                        if (rowIndex < element.rows - 1) {
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}