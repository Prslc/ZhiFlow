package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder
import com.prslc.zhiflow.ui.theme.TextStyles

/**
 * Adds the colours of [from]'s annotated ranges to this builder.
 *
 * A body is parsed once and drawn in whichever mode the reader is in, so the three marks whose
 * appearance follows the theme -- a link, an inline `code`, a `reference` -- travel as annotations
 * rather than as spans. Everything that draws one of those strings puts them on here, which is also
 * what keeps two screens from styling a link differently.
 *
 * Take [from] as the string the annotations came off, not the builder's own contents: `ZRichText`
 * turns its `URL` ranges into link annotations as it copies them, so by the end of that pass the
 * tags this reads are no longer there.
 */
fun AnnotatedString.Builder.applyThemeSpans(from: AnnotatedString, isDark: Boolean) {
    val styles = listOf(
        AnnotatedStringBuilder.URL_TAG to TextStyles.linkStyle(isDark),
        AnnotatedStringBuilder.CODE_TAG to TextStyles.codeStyle(isDark),
        AnnotatedStringBuilder.REFERENCE_TAG to TextStyles.referenceStyle(isDark),
    )

    styles.forEach { (tag, style) ->
        from.getStringAnnotations(tag, 0, from.length).forEach { addStyle(style, it.start, it.end) }
    }
}

/**
 * [applyThemeSpans] for a string that is drawn as it came out of the parse.
 *
 * The styles and the paragraph styles are copied by hand: `Builder.append(AnnotatedString)` carries
 * the text and the annotations over but not those two, which is why `ZRichText` copies them by hand
 * as well.
 */
@Composable
fun AnnotatedString.withThemeSpans(): AnnotatedString {
    val isDark = isSystemInDarkTheme()
    return remember(this, isDark) {
        buildAnnotatedString {
            append(this@withThemeSpans)
            this@withThemeSpans.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            this@withThemeSpans.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
            applyThemeSpans(this@withThemeSpans, isDark)
        }
    }
}
