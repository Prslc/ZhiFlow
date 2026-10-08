package com.prslc.zhiflow.data.remote.parser

import android.text.Html
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.prslc.zhiflow.data.remote.parser.emoji.EmojiParser

private val SEARCH_WORD_REGEX = """<a[^>]+class="[^"]*search_word[^"]*"[^>]*>(.*?)</a>""".toRegex()

private val LINK_COLOR = Color(0xFF1E88E5)

/**
 * The HTML the backend writes, as text this app can draw: entities decoded, emoji as images, and the
 * links it carries kept as spans to tap.
 *
 * The search links it injects into the wording come off first. The word belongs to the sentence; the
 * link is to a search this app does not run, and it was arriving as a blue link that did nothing.
 */
internal fun htmlToAnnotatedString(html: String): AnnotatedString {
    val spanned: Spanned = Html.fromHtml(
        SEARCH_WORD_REGEX.replace(html) { it.groupValues[1] },
        Html.FROM_HTML_MODE_COMPACT,
    )

    return buildAnnotatedString {
        append(EmojiParser.parse(spanned.toString().trim()))

        spanned.getSpans(0, spanned.length, URLSpan::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            addStringAnnotation("URL", span.url, start, end)
            addStyle(SpanStyle(color = LINK_COLOR), start, end)
        }
    }
}
