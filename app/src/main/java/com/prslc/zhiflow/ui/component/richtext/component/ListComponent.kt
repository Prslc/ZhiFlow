package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.richtext.ZRichText

/**
 * One list item. Its bullet is drawn rather than written as a "•": a glyph's size comes from the
 * font and it cannot be centred on the line, which is where a bullet reads as belonging to the
 * text. Hence a fixed-size mark inside a box the height of the first line.
 *
 * @param element The item segment. Its level sets the indent, at 12dp for each level past the
 *   first; the bullet itself is drawn in a 24dp column that the text is laid out beside.
 * @param modifier Applied to the row ahead of the `fillMaxWidth` it adds itself.
 */
@Composable
fun BulletItemRow(
    element: RichTextElement.BulletItem,
    modifier: Modifier = Modifier
) {
    val indentation = (maxOf(0, element.level - 1) * 12).dp
    val markerWidth = 24.dp
    val style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
    val dotSize = 6.dp
    val firstLineHeight = with(LocalDensity.current) { 22.sp.toDp() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indentation, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        when {
            element.isOrdered -> Text(
                text = "${element.index}.",
                style = style.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.width(markerWidth).alignByBaseline(),
            )

            else -> Box(
                modifier = Modifier.width(markerWidth).height(firstLineHeight),
                contentAlignment = Alignment.Center,
            ) {
                // A sub-list changes its mark as well as its indent, the way a nested ul does on
                // the web: filled disc, then ring, then square, and back round.
                val color = MaterialTheme.colorScheme.primary
                when ((element.level - 1) % 3) {
                    1 -> Box(modifier = Modifier.size(dotSize).border(1.5.dp, color, CircleShape))
                    2 -> Box(modifier = Modifier.size(5.dp).background(color))
                    else -> Box(modifier = Modifier.size(dotSize).background(color, CircleShape))
                }
            }
        }
        ZRichText(
            content = element.content,
            inlineMetas = element.inlineMetas,
            style = style,
            modifier = Modifier.weight(1f).alignByBaseline(),
        )
    }
}

/**
 * The footnote list a body ends with: the entries the `reference` marks point at.
 *
 * The heading is this component's own, so the items arrive without their numbers.
 *
 * @param items The reference entries, drawn in the order the marks gave them.
 * @param modifier Applied to the column ahead of its 8dp of vertical padding.
 */
@Composable
fun ReferenceSection(
    items: List<RichTextElement.ParsedText>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.richtext_references),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(bottom = 4.dp),
        )

        items.forEachIndexed { index, item ->
            val fullAnnotatedString = remember(item.content) {
                buildAnnotatedString {
                    append("${index + 1}. ")
                    // The number shifts every range, and append carries only the text and the
                    // annotations over -- the two style lists have to be copied at that offset.
                    val offset = length
                    append(item.content)
                    item.content.spanStyles.forEach {
                        addStyle(it.item, offset + it.start, offset + it.end)
                    }
                    item.content.paragraphStyles.forEach {
                        addStyle(it.item, offset + it.start, offset + it.end)
                    }
                }
            }
            ZRichText(
                content = fullAnnotatedString,
                inlineMetas = item.inlineMetas,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 20.sp,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}
