package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.runtime.Composable
import com.prslc.zhiflow.parser.RichTextElement

@Composable
fun RichTextSingleElement(
    element: RichTextElement,
    onImageClick: (String) -> Unit,
) {
    when (element) {
        is RichTextElement.ParsedText -> { FormulaTextSection(element, onImageClick) }
        is RichTextElement.Heading -> Heading(element)
        is RichTextElement.FormulaBlock -> LatexComponent(element.data, isInline = false)
        is RichTextElement.Image -> ImageComponent(element.data, onImageClick)
        is RichTextElement.Code -> CodeBlock(element.code, element.lang)
        is RichTextElement.BulletItem -> BulletItemRow(element)
        is RichTextElement.Blockquote -> BlockquoteComponent(element)
        is RichTextElement.Card -> CardComponent(element)
        is RichTextElement.Table -> TableComponent(element)
        is RichTextElement.Reference -> ReferenceSection(element.items)
        is RichTextElement.Divider -> Divider()
    }
}
