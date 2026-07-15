package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.richtext.component.BlockquoteComponent
import com.prslc.zhiflow.ui.component.richtext.component.BulletItemRow
import com.prslc.zhiflow.ui.component.richtext.component.CardComponent
import com.prslc.zhiflow.ui.component.richtext.component.CodeBlock
import com.prslc.zhiflow.ui.component.richtext.component.Divider
import com.prslc.zhiflow.ui.component.richtext.component.FormulaTextSection
import com.prslc.zhiflow.ui.component.richtext.component.Heading
import com.prslc.zhiflow.ui.component.richtext.component.ImageComponent
import com.prslc.zhiflow.ui.component.richtext.component.LatexComponent
import com.prslc.zhiflow.ui.component.richtext.component.ReferenceSection
import com.prslc.zhiflow.ui.component.richtext.component.TableComponent

@Composable
fun RichTextSingleElement(
    element: RichTextElement,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit,
) {
    when (element) {
        is RichTextElement.ParsedText -> {
            FormulaTextSection(element, onImageClick, modifier)
        }
        is RichTextElement.Heading -> Heading(element, modifier)
        is RichTextElement.FormulaBlock -> LatexComponent(element.data, modifier, isInline = false)
        is RichTextElement.Image -> ImageComponent(element.data, onImageClick, modifier)
        is RichTextElement.Code -> CodeBlock(element.code, element.lang, modifier)
        is RichTextElement.BulletItem -> BulletItemRow(element, modifier)
        is RichTextElement.Blockquote -> BlockquoteComponent(element, modifier)
        is RichTextElement.Card -> when (element.cardType) {
            "reward_tail_truncate" -> { /* TODO: custom rendering */ }
            "free_column_card" -> { /* TODO: custom rendering */ }
            else -> CardComponent(element, modifier)
        }
        is RichTextElement.Table -> TableComponent(element, modifier)
        is RichTextElement.Reference -> ReferenceSection(element.items, modifier)
        is RichTextElement.Divider -> Divider(modifier)
    }
}
