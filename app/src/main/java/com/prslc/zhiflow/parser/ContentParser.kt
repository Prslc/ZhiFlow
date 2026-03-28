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

        val aTagRegex = """<a[^>]+class="[^"]*comment_img[^"]*"[^>]*>.*?</a>""".toRegex()
        val hrefRegex = """href="([^"]+)"""".toRegex()
        val widthRegex = """data-width="(\d+)"""".toRegex()
        val heightRegex = """data-height="(\d+)"""".toRegex()

        var processedHtml = html

        aTagRegex.findAll(html).forEach { match ->
            val fullTag = match.value

            val url = hrefRegex.find(fullTag)?.groupValues?.get(1) ?: ""
            val w = widthRegex.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val h = heightRegex.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0

            if (url.isNotEmpty()) {
                extractedImages.add(ZhihuImage(urls = listOf(url), width = w, height = h))
            }

            processedHtml = processedHtml.replace(fullTag, "")
        }

        val cleanText = Html.fromHtml(processedHtml, Html.FROM_HTML_MODE_COMPACT)
            .toString()
            .trim()

        return CommentContent(
            text = cleanText,
            images = extractedImages
        )
    }
}