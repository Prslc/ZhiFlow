package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        elements.forEach { element ->
            when (element) {
                is RichTextElement.Text -> {
                    RichTextComponent(
                        content = element.content,
                        onImageClick = onImageClick,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                is RichTextElement.Heading -> {
                    Heading(element)
                }

                is RichTextElement.Code -> {
                    CodeBlock(
                        code = element.code,
                        lang = element.lang
                    )
                }

                is RichTextElement.BulletItem -> {
                    BulletItemRow(element)
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
                    BlockquoteComponent(element.content)
                }

                is RichTextElement.Table -> {
                    TableComponent(element)
                }

                is RichTextElement.FormulaBlock -> {
                    LatexComponent(
                        formula = element.data,
                        isInline = false,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                is RichTextElement.Card -> {
                    CardComponent(element)
                }

                is RichTextElement.Divider -> {
                    Divider()
                }
            }
        }
    }
}

