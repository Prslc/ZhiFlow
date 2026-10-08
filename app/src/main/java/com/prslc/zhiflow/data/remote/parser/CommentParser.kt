package com.prslc.zhiflow.data.remote.parser

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.comment.CommentContent
import com.prslc.zhiflow.data.model.content.ZhihuImage

@Immutable
object CommentParser {
    /**
     * Parse Zhihu comment HTML into a [CommentContent] with extracted images.
     *
     * Strips image `<a>` tags from the HTML, extracts them as [ZhihuImage], and renders what is left
     * through [htmlToAnnotatedString].
     */
    fun parse(html: String): CommentContent {
        val extractedImages = mutableListOf<ZhihuImage>()

        val aTagRegex = """<a[^>]+href="([^"]+)"[^>]*>(.*?)</a>""".toRegex()
        val hrefRegex = """href="([^"]+)"""".toRegex()
        val widthRegex = """data-width="(\d+)"""".toRegex()
        val heightRegex = """data-height="(\d+)"""".toRegex()

        var processedHtml = html
        aTagRegex.findAll(html).forEach { match ->
            val fullTag = match.value
            val url = hrefRegex.find(fullTag)?.groupValues?.get(1) ?: ""
            val isImage = fullTag.contains("comment_img") || fullTag.contains("comment_gif")

            if (isImage && url.isNotEmpty()) {
                val w = widthRegex.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val h = heightRegex.find(fullTag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val isGif = fullTag.contains("comment_gif") || url.lowercase().contains(".gif")

                extractedImages.add(
                    ZhihuImage(
                        urls = listOf(url),
                        width = w,
                        height = h,
                        isGif = isGif,
                        description = "",
                    )
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
