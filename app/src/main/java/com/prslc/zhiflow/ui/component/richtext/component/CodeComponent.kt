package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.core.utils.platform.rememberCopyTextToClipboard
import com.prslc.zhiflow.data.remote.parser.model.CodeToken
import com.prslc.zhiflow.ui.theme.SyntaxColors

/**
 * A block of code, on a card of its own.
 *
 * The block carries its own copy button and scrolls sideways, so selection is off inside it: a drag
 * would fight the scroll, and there is nothing to select that the button does not already take.
 *
 * @param code The code as the parser found it, with the fence already off.
 * @param lang The language the fence named, or null when it named none. It is drawn upper-cased,
 *   with `CODE` standing in where there is nothing to name.
 * @param tokens The runs the tokenizer found, never overlapping and carrying no colour of their
 *   own. A kind the palette leaves unspecified keeps the text's colour, so a block the tokenizer
 *   had nothing to say about draws exactly as it did before there was a tokenizer.
 * @param modifier Applied to the card ahead of the `fillMaxWidth` it adds itself.
 */
@Composable
fun CodeBlock(
    code: String,
    lang: String?,
    tokens: List<CodeToken>,
    modifier: Modifier = Modifier
) {
    val copyText = rememberCopyTextToClipboard()
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    // Keyed on the mode: the block is parsed once and cached across both, so the colours are the
    // one part of it that has to be rebuilt when the reader switches.
    val styledCode = remember(code, tokens, isDark) {
        buildAnnotatedString {
            append(code)
            tokens.forEach { token ->
                val color = SyntaxColors.of(token.kind, isDark)
                if (color != Color.Unspecified) {
                    addStyle(SpanStyle(color = color), token.start, token.end)
                }
            }
        }
    }

    // Has its own whole-block copy button and scrolls horizontally; selection would fight that.
    DisableSelection {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lang?.uppercase() ?: "CODE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        )
                    )

                    IconButton(
                        onClick = { copyText(code) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                Box(modifier = Modifier.horizontalScroll(scrollState)) {
                    Text(
                        text = styledCode,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        ),
                        softWrap = false,
                    )
                }
            }
        }
    }
}
