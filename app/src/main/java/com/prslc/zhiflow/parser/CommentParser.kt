package com.prslc.zhiflow.parser

import android.text.Html
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.prslc.zhiflow.data.model.CommentContent
import com.prslc.zhiflow.data.model.ZhihuImage

fun commentParse(html: String): CommentContent {
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

    // Text
    val spanned = Html.fromHtml(processedHtml, Html.FROM_HTML_MODE_COMPACT)

    val annotatedText = buildAnnotatedString {
        val rawText = spanned.toString().trim()
        append(EmojiParser.parse(rawText))

        if (spanned is android.text.Spanned) {
            val spans =
                spanned.getSpans(0, spanned.length, android.text.style.URLSpan::class.java)
            spans.forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)

                addStringAnnotation("URL", span.url, start, end)
                addStyle(
                    SpanStyle(color = Color(0xFF1E88E5)),
                    start, end
                )
            }
        }
    }

    return CommentContent(text = annotatedText, images = extractedImages)
}