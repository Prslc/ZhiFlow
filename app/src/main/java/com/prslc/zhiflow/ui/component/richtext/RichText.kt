package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.model.content.ZhihuImage
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

/**
 * Draws one parsed element of a body.
 *
 * This is the pipeline's dispatcher: a screen walks its element list and hands each one here, and
 * nothing above this point has to know which kind it is.
 *
 * @param element The element to draw.
 * @param onImageClick Called with the image that was tapped, for the kinds that carry one.
 * @param modifier Applied to whichever composable the element turns out to need.
 * @param segmentLikes The live like state of the paragraph's `seg_like` ranges, keyed by the
 *   range's own key. Empty for every element that has none.
 * @param onSegmentLikeClick Called with a range's key when the reader opens its panel.
 */
@Composable
fun RichTextSingleElement(
    element: RichTextElement,
    onImageClick: (ZhihuImage) -> Unit,
    modifier: Modifier = Modifier,
    segmentLikes: Map<String, SegmentLikeTarget> = emptyMap(),
    onSegmentLikeClick: (String) -> Unit = {},
) {
    when (element) {
        is RichTextElement.ParsedText -> {
            FormulaTextSection(
                element = element,
                modifier = modifier,
                segmentLikes = segmentLikes,
                onSegmentLikeClick = onSegmentLikeClick,
            )
        }
        is RichTextElement.Heading -> Heading(element, modifier)
        is RichTextElement.FormulaBlock -> LatexComponent(element.data, modifier, isInline = false)
        is RichTextElement.Image -> ImageComponent(element.data, onImageClick, modifier)
        is RichTextElement.Code -> CodeBlock(element.code, element.lang, element.tokens, modifier)
        is RichTextElement.BulletItem -> BulletItemRow(element, modifier)
        is RichTextElement.Blockquote -> BlockquoteComponent(
            element = element,
            modifier = modifier,
            segmentLikes = segmentLikes,
            onSegmentLikeClick = onSegmentLikeClick,
        )
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
