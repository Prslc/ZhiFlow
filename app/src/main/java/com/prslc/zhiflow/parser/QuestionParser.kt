package com.prslc.zhiflow.parser

import android.text.Html
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.prslc.zhiflow.data.model.ZhihuImage
import com.prslc.zhiflow.parser.model.DetailElement

object QuestionParser {
    private val figureRegex = """<figure[^>]*>(.*?)</figure>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val imgRegex = """<img[^>]+src="([^"]+)"[^>]*data-rawwidth="(\d+)"[^>]*data-rawheight="(\d+)"[^>]*>""".toRegex()
    private val captionRegex = """<figcaption>(.*?)</figcaption>""".toRegex()

    fun parse(html: String?): List<DetailElement> {
        if (html.isNullOrEmpty()) return emptyList()

        val elements = mutableListOf<DetailElement>()
        var lastIndex = 0

        figureRegex.findAll(html).forEach { match ->
            val preText = html.substring(lastIndex, match.range.first).trim()
            if (preText.isNotEmpty()) {
                elements.add(DetailElement.Text(renderHtmlText(preText)))
            }

            val figureContent = match.groupValues[1]
            val imgMatch = imgRegex.find(figureContent)
            val caption = captionRegex.find(figureContent)?.groupValues?.get(1) ?: ""

            imgMatch?.let {
                val url = it.groupValues[1]
                val w = it.groupValues[2].toIntOrNull() ?: 0
                val h = it.groupValues[3].toIntOrNull() ?: 0

                elements.add(DetailElement.Image(
                    ZhihuImage(
                        urls = listOf(url),
                        width = w,
                        height = h,
                        description = caption,
                        isGif = url.lowercase().contains(".gif")
                    )
                ))
            }
            lastIndex = match.range.last + 1
        }

        val postText = html.substring(lastIndex).trim()
        if (postText.isNotEmpty()) {
            elements.add(DetailElement.Text(renderHtmlText(postText)))
        }

        return elements
    }

    private fun renderHtmlText(html: String): AnnotatedString {
        val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        return spanned.toAnnotatedString()
    }
}

fun Spanned.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    val rawText = this@toAnnotatedString.toString()
    val emojiText = EmojiParser.parse(rawText)
    append(emojiText)

    // link
    val spans = getSpans(0, length, URLSpan::class.java)
    spans.forEach { span ->
        val start = getSpanStart(span)
        val end = getSpanEnd(span)

        addStringAnnotation("URL", span.url, start, end)
        addStyle(
            SpanStyle(color = Color(0xFF1E88E5)),
            start, end
        )
    }
}