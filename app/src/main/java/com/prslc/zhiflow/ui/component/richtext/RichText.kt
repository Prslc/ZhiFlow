package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prslc.zhiflow.parser.model.RichTextElement

@Composable
fun RichTextSingleElement(
    element: RichTextElement,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit,
) {
    when (element) {
        is RichTextElement.ParsedText -> { FormulaTextSection(element, onImageClick, modifier) }
        is RichTextElement.Heading -> Heading(element, modifier)
        is RichTextElement.FormulaBlock -> LatexComponent(element.data, modifier, isInline = false)
        is RichTextElement.Image -> ImageComponent(element.data, onImageClick, modifier)
        is RichTextElement.Code -> CodeBlock(element.code, element.lang, modifier)
        is RichTextElement.BulletItem -> BulletItemRow(element, modifier)
        is RichTextElement.Blockquote -> BlockquoteComponent(element, modifier)
        is RichTextElement.Card -> CardComponent(element, modifier)
        is RichTextElement.Table -> TableComponent(element, modifier)
        is RichTextElement.Reference -> ReferenceSection(element.items, modifier)
        is RichTextElement.Divider -> Divider(modifier)
    }
}
