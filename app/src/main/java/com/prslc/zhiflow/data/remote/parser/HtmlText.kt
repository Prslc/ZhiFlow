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
 *
 * Each link is annotated as its run is emitted, and the runs are emitted through the emoji pass one
 * at a time, so nothing can shorten the ground an offset was measured on.
 */
internal fun htmlToAnnotatedString(html: String): AnnotatedString {
    val spanned: Spanned = Html.fromHtml(
        SEARCH_WORD_REGEX.replace(html) { it.groupValues[1] },
        Html.FROM_HTML_MODE_COMPACT,
    )

    val full = spanned.toString()
    val text = full.trim()
    // The spans were measured over `full`, an origin that is no longer the first character drawn.
    val trimmed = full.length - full.trimStart().length

    return buildAnnotatedString {
        var cursor = 0

        spanned.getSpans(0, spanned.length, URLSpan::class.java)
            .sortedBy { spanned.getSpanStart(it) }
            .forEach { span ->
                val start = (spanned.getSpanStart(span) - trimmed).coerceIn(cursor, text.length)
                val end = (spanned.getSpanEnd(span) - trimmed).coerceIn(start, text.length)

                append(EmojiParser.parse(text.substring(cursor, start)))

                val linkStart = length
                append(EmojiParser.parse(text.substring(start, end)))
                addStringAnnotation("URL", span.url, linkStart, length)
                addStyle(SpanStyle(color = LINK_COLOR), linkStart, length)

                cursor = end
            }

        append(EmojiParser.parse(text.substring(cursor)))
    }
}
