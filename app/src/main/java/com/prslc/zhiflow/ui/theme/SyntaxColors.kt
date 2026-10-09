package com.prslc.zhiflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.prslc.zhiflow.data.remote.parser.model.CodeTokenKind

/**
 * The colours the runs of a code block are drawn in.
 *
 * A palette of its own rather than roles off the colour scheme: eight token kinds do not fit the
 * scheme's few accents, and code colour is a convention the reader brings with them from every
 * other editor they have read. That is also why it ignores dynamic colour — these hues have to stay
 * apart from each other and from the comments, which a wallpaper-derived scheme cannot promise.
 *
 * Every colour clears 4.5:1 against the card it is drawn on: the dark arm against the measured
 * `#1A1F24`, the light one against a card anywhere in `#F0EFF2`..`#F8F7FA`.
 */
@Immutable
object SyntaxColors {

    /**
     * The colour for a run of [kind], or [Color.Unspecified] to leave it in the text's own colour.
     *
     * The two symbol kinds take the surrounding colour because they are what the code is made of
     * rather than what it says — `(`, `=` and `,` are the sentence's punctuation, and colouring
     * them would only make the block busier. A renderer can read [Color.Unspecified] as "draw
     * nothing here" and leave those runs out of its span list.
     */
    fun of(kind: CodeTokenKind, isDark: Boolean): Color = when (kind) {
        CodeTokenKind.Keyword -> if (isDark) Color(0xFFC678DD) else Color(0xFFA626A4)
        CodeTokenKind.String -> if (isDark) Color(0xFF98C379) else Color(0xFF2B7230)
        CodeTokenKind.Literal -> if (isDark) Color(0xFFD19A66) else Color(0xFF8A5A00)
        CodeTokenKind.Annotation -> if (isDark) Color(0xFFE5C07B) else Color(0xFF8A6100)
        // One grey for both: a comment reads as a comment however it was opened.
        CodeTokenKind.Comment,
        CodeTokenKind.MultilineComment -> if (isDark) Color(0xFF8B949E) else Color(0xFF5F646C)

        CodeTokenKind.Mark,
        CodeTokenKind.Punctuation -> Color.Unspecified
    }
}
