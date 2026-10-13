package com.prslc.zhiflow.ui.page.comment

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.component.common.BadgeLabel
import com.prslc.zhiflow.ui.component.richtext.component.inlinePlaceholder
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import com.prslc.zhiflow.ui.preview.sampleParagraph

/**
 * The id the topping badge's placeholder registers under, in the text of a comment the author
 * pinned. `appendInlineContent` writes it as an annotation over the range the badge replaces, and
 * the map it is looked up in is the one the text is drawn with.
 */
internal const val TOPPING_BADGE = "topping_badge"

/** The room [BadgeLabel] leaves around its words, which the placeholder's box has to hold too. */
private val ToppingBadgePadding = 4.dp
private val ToppingBadgeVerticalPadding = 2.dp

/**
 * [this] text with the badge's placeholder in front of it, and the space that keeps the words off
 * the badge.
 *
 * The badge is found by the annotation [appendInlineContent] writes, not by the string it is named
 * after -- so the marker has to go in through here, and the text a comment draws is the one that
 * carries it. What it falls back to, where no badge is drawn for it, is the word the badge says.
 */
internal fun AnnotatedString.withToppingBadge(label: String): AnnotatedString = buildAnnotatedString {
    appendInlineContent(TOPPING_BADGE, alternateText = label)
    append(' ')
    append(this@withToppingBadge)
}

/**
 * The topping badge, as the inline content that draws it on the first line of a comment.
 *
 * To the text the badge is a box, and the box is fixed before the text is laid out -- so its size
 * has to be known in advance. The words are measured here in the style the badge draws them in,
 * the badge's own padding added, and [inlinePlaceholder] hands the result over in sp: the box
 * covers the same pixels whatever font scale the reader is on, which is what keeps it and the
 * badge it stands for the same size.
 */
@Composable
internal fun rememberToppingBadge(label: String): InlineTextContent {
    val style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    return remember(label, style, measurer, density) {
        val measured = measurer.measure(label, style = style).size
        InlineTextContent(
            placeholder = inlinePlaceholder(
                density = density,
                widthDp = with(density) { measured.width.toDp().value } +
                    ToppingBadgePadding.value * 2,
                heightDp = with(density) { measured.height.toDp().value } +
                    ToppingBadgeVerticalPadding.value * 2,
            ),
            children = {
                BadgeLabel(
                    text = label,
                    contentColor = MaterialTheme.colorScheme.primary,
                    outlined = true,
                )
            },
        )
    }
}

/** The badge on the first line of a paragraph, which is where a comment wears it. */
@AppPreviews
@Composable
private fun ToppingBadgePreview() {
    val label = stringResource(R.string.comment_topping)
    PreviewSurface {
        Text(
            text = sampleParagraph.content.withToppingBadge(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            inlineContent = mapOf(TOPPING_BADGE to rememberToppingBadge(label)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        )
    }
}
