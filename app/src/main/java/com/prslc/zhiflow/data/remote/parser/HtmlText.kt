package com.prslc.zhiflow.data.remote.parser

import android.graphics.Typeface
import android.text.Html
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.prslc.zhiflow.data.remote.parser.emoji.EmojiParser
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder

private val SEARCH_WORD_REGEX = """<a[^>]+class="[^"]*search_word[^"]*"[^>]*>(.*?)</a>""".toRegex()

/**
 * The HTML the backend writes, as text this app can draw: entities decoded, emoji as images, and
 * the links it carries kept as spans to tap.
 *
 * The search links it injects into the wording come off first. The word belongs to the sentence;
 * the link is to a search this app does not run, and it was arriving as a blue link that did
 * nothing.
 *
 * Each link is annotated as its run is emitted, and the runs are emitted through the emoji pass one
 * at a time, so nothing can shorten the ground an offset was measured on. The leading trim is
 * carried as an offset for the same reason: Html measures its spans over the untrimmed text.
 */
internal fun htmlToAnnotatedString(html: String): AnnotatedString {
    val spanned: Spanned = Html.fromHtml(
        SEARCH_WORD_REGEX.replace(html) { it.groupValues[1] },
        Html.FROM_HTML_MODE_COMPACT,
    )

    val full = spanned.toString()
    val text = full.trim()
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
                addStringAnnotation(AnnotatedStringBuilder.URL_TAG, span.url, linkStart, length)

                cursor = end
            }

        append(EmojiParser.parse(text.substring(cursor)))
    }
}

/**
 * The plain text of a field the backend writes HTML into.
 *
 * For the fields search marks up that this app does not draw as rich text — an author's name,
 * their headline. The `<em>` around a matched word comes off with the rest: a name is not a place
 * to mark what the query matched.
 */
internal fun htmlToPlainText(html: String): String = Html.fromHtml(
    SEARCH_WORD_REGEX.replace(html) { it.groupValues[1] },
    Html.FROM_HTML_MODE_COMPACT,
).toString().trim()

/**
 * A piece of a search result, with the words the query matched marked.
 *
 * The endpoint wraps those words in `<em>`, which [Html.fromHtml] turns into an italic span -- not
 * what marks a match in a list of results, and not what this app draws anywhere else. They are
 * carried as a weight instead. It is not only the title that arrives this way; the excerpt does
 * too, whatever the field's documentation says. Links the backend injects come off the same way
 * they do in [htmlToAnnotatedString]: the words stay, the link to a search is dropped. The leading
 * trim is carried as an offset: Html measures its spans over the untrimmed text.
 */
internal fun searchTextToAnnotatedString(html: String): AnnotatedString {
    val spanned: Spanned = Html.fromHtml(
        SEARCH_WORD_REGEX.replace(html) { it.groupValues[1] },
        Html.FROM_HTML_MODE_COMPACT,
    )

    val full = spanned.toString()
    val text = full.trim()
    val trimmed = full.length - full.trimStart().length

    return buildAnnotatedString {
        var cursor = 0

        spanned.getSpans(0, spanned.length, StyleSpan::class.java)
            .filter { it.style == Typeface.ITALIC }
            .sortedBy { spanned.getSpanStart(it) }
            .forEach { span ->
                val start = (spanned.getSpanStart(span) - trimmed).coerceIn(cursor, text.length)
                val end = (spanned.getSpanEnd(span) - trimmed).coerceIn(start, text.length)

                append(EmojiParser.parse(text.substring(cursor, start)))

                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(EmojiParser.parse(text.substring(start, end)))
                }

                cursor = end
            }

        append(EmojiParser.parse(text.substring(cursor)))
    }
}
