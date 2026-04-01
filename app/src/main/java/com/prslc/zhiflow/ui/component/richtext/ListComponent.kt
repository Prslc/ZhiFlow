package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.parser.RichTextElement

@Composable
fun BulletItemRow(
    item: RichTextElement.BulletItem,
    uriHandler: UriHandler
) {
    val indentation = (maxOf(0, item.level - 1) * 12).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentation, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = if (item.isOrdered) "${item.index}." else "•",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.widthIn(min = 16.dp)
        )

        ClickableText(
            content = item.content,
            onClick = { url -> uriHandler.openUri(url) },
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp)
        )
    }
}

@Composable
fun ReferenceSection(items: List<AnnotatedString>) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.richtext_references),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        items.forEachIndexed { index, content ->
            val fullAnnotatedString = remember(content) {
                buildAnnotatedString {
                    append("${index + 1}. ")
                    append(content)
                }
            }

            ClickableText(
                content = fullAnnotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 20.sp,
                    color = Color.Gray
                ),
                onClick = { url ->
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        throw e
                    }
                }
            )
        }
    }
}