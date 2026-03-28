package com.prslc.zhiflow.parser

import android.text.Html
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.prslc.zhiflow.data.model.Paragraph
import com.prslc.zhiflow.data.model.Segment
import com.prslc.zhiflow.data.model.ZhihuImage

data class CommentContent(
    val text: String,
    val images: List<ZhihuImage>
)

sealed interface RichTextElement {
    data class Text(val content: AnnotatedString) : RichTextElement
    data class Image(val data: ZhihuImage) : RichTextElement
    data object Divider : RichTextElement
}

object ContentParser {
    private val commentImgRegex =
        """<a[^>]+class="comment_img"[^>]+href="([^"]+)"[^>]*data-width="(\d+)"[^>]*data-height="(\d+)"""".toRegex()

    fun transform(segments: List<Segment>): List<RichTextElement> {
        return segments.mapNotNull { segment ->
            when (segment.type) {
                "paragraph" -> {
                    val text = segment.paragraph?.text ?: ""
                    if (text.trim() == "---") {
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

    fun parseParagraph(paragraph: Paragraph?): AnnotatedString {
        val rawText = paragraph?.text ?: ""
        return buildAnnotatedString {
            append(rawText)
        }
    }

    /**
     * Specifically for parsing Zhihu V5 Comment HTML
     */
    fun parseCommentHtml(html: String): CommentContent {
        val extractedImages = mutableListOf<ZhihuImage>()

        // Extract Image metadata using Regex
        commentImgRegex.findAll(html).forEach { match ->
            val url = match.groupValues[1]
            val w = match.groupValues[2].toIntOrNull() ?: 0
            val h = match.groupValues[3].toIntOrNull() ?: 0

            extractedImages.add(
                ZhihuImage(
                    urls = listOf(url),
                    width = w,
                    height = h
                )
            )
        }

        // Strip HTML tags and entities
        val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)

        // Remove the "[图片]" placeholder text often found in <a> tags
        val cleanText = spanned.toString()
            .replace("[图片]", "")
            .trim()

        return CommentContent(
            text = cleanText,
            images = extractedImages
        )
    }
}