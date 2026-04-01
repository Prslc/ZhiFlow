package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.BuildConfig
import com.prslc.zhiflow.data.model.Segment
import com.prslc.zhiflow.parser.ContentParser
import com.prslc.zhiflow.parser.RichTextElement

@Composable
fun RichText(
    segments: List<Segment>,
    onImageClick: (String) -> Unit
) {
    val elements = remember(segments) {
        ContentParser.transform(segments)
    }

    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        elements.forEach { element ->
            when (element) {
                is RichTextElement.Text -> {
                    ClickableText(
                        content = element.content,
                        onClick = { url ->
                            uriHandler.openUri(url)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp,
                            letterSpacing = 0.25.sp
                        ),
                    )
                }

                is RichTextElement.Heading -> {
                    Heading(
                        element = element
                    )
                }

                is RichTextElement.Code -> {
                    CodeBlock(
                        code = element.code,
                        lang = element.lang
                    )
                }

                is RichTextElement.BulletItem -> {
                    BulletItemRow(
                        item = element,
                        uriHandler = uriHandler
                    )
                }

                is RichTextElement.Image -> {
                    ImageComponent(
                        image = element.data,
                        onImageClick = onImageClick
                    )
                }

                is RichTextElement.Reference -> {
                    ReferenceSection(element.items)
                }

                is RichTextElement.Blockquote -> {
                    BlockquoteComponent(
                        content = element.content,
                        uriHandler = uriHandler
                    )
                }

                is RichTextElement.Table -> {
                    TableComponent(
                        element = element
                    )
                }

                is RichTextElement.Divider -> {
                    Divider()
                }

                else -> {
                    if (BuildConfig.DEBUG) {
                        Text(
                            text = "Unparsed: ${element::class.simpleName}",
                            color = Color.Red,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

