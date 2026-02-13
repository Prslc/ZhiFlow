package com.prslc.zhiflow.parser

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.prslc.zhiflow.data.model.Paragraph
import com.prslc.zhiflow.data.model.Segment
import com.prslc.zhiflow.data.model.ZhihuImage

sealed interface RichTextElement {
    data class Text(val content: AnnotatedString) : RichTextElement
    data class Image(val data: ZhihuImage) : RichTextElement
    object Divider : RichTextElement
}

object ContentParser {
    fun transform(segments: List<Segment>): List<RichTextElement> {
        return segments.mapNotNull { segment ->
            when (segment.type) {
                "paragraph" -> {
                    val text = segment.paragraph?.text
                    if (text?.trim() == "---") {
                        RichTextElement.Divider
                    } else {
                        RichTextElement.Text(parseParagraph(segment.paragraph))
                    }
                }
                "image" -> {
                    segment.image?.let { RichTextElement.Image(it) }
                }
                else -> null
            }
        }
    }

    // text parser
    fun parseParagraph(paragraph: Paragraph?): AnnotatedString {
        val rawText = paragraph?.text ?: ""
        return buildAnnotatedString {
            append(rawText)
        }
    }
}