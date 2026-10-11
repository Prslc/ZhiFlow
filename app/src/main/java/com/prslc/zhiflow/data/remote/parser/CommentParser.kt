package com.prslc.zhiflow.data.remote.parser

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.comment.CommentContent
import com.prslc.zhiflow.data.model.content.ZhihuImage

@Immutable
object CommentParser {

    private val A_TAG_REGEX = """<a[^>]+href="([^"]+)"[^>]*>(.*?)</a>""".toRegex()
    private val HREF_REGEX = """href="([^"]+)"""".toRegex()
    private val WIDTH_REGEX = """data-width="(\d+)"""".toRegex()
    private val HEIGHT_REGEX = """data-height="(\d+)"""".toRegex()

    /**
     * Parse Zhihu comment HTML into a [CommentContent] with extracted images.
     *
     * Strips image `<a>` tags from the HTML, extracts them as [ZhihuImage], and renders what is
     * left through [htmlToAnnotatedString].
     */
    fun parse(html: String): CommentContent {
        val extractedImages = mutableListOf<ZhihuImage>()

        var processedHtml = html
        A_TAG_REGEX.findAll(html).forEach { match ->
            val fullTag = match.value
            val url = HREF_REGEX.find(fullTag)?.groupValues?.get(1) ?: ""
            val isImage = fullTag.contains("comment_img") || fullTag.contains("comment_gif")

            if (isImage && url.isNotEmpty()) {
                val w = WIDTH_REGEX.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val h = HEIGHT_REGEX.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val isGif = fullTag.contains("comment_gif") || url.lowercase().contains(".gif")

                extractedImages.add(
                    ZhihuImage(
                        urls = listOf(url),
                        width = w,
                        height = h,
                        isGif = isGif,
                        description = "",
                    ),
                )
                processedHtml = processedHtml.replace(fullTag, "")
            }
        }

        return CommentContent(
            text = htmlToAnnotatedString(processedHtml),
            images = extractedImages,
        )
    }
}
