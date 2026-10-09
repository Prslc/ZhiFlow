package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.common.contentTypeConfig
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.theme.TextStyles

/**
 * A card a body embeds: a link out to other content.
 *
 * It is a teaser and not a copy -- two lines of title, and a meta line under them carrying the
 * counts and the word for what the link points at, or the address itself when it leads off Zhihu
 * -- and there is no text to select, so selection is off and the whole surface is the tap target.
 *
 * @param element The card segment. Its url is what a tap opens, through the navigator.
 * @param modifier Applied to the surface, ahead of the `fillMaxWidth` and the 4dp of vertical
 *   padding it adds itself.
 */
@Composable
fun CardComponent(
    element: RichTextElement.Card,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    val hasImage = !element.cover.isNullOrBlank()

    // Tappable teaser: title clipped to 2 lines, the meta line to 1 — no full text to select.
    DisableSelection {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                    navigator.handleUrl(element.url)
                },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image
                if (hasImage) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = element.cover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                // Text
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = element.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = TextStyles.cardTitleBold,
                            lineHeight = TextStyles.cardTitleLineHeight
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    CardMetaLine(element, Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/**
 * The line under a card's title: what the card points at, or the address of the page it leads to.
 */
@Composable
private fun CardMetaLine(
    element: RichTextElement.Card,
    modifier: Modifier = Modifier
) {
    val style = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = TextStyles.cardDescSize
    )

    val link = element.externalLink
    if (link != null) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = link.substringAfter("://"),
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        val typeLabel = stringResource(contentTypeConfig(element.contentType).labelResId)
        val metaLine = if (element.voteCount > 0 || element.commentCount > 0) {
            val counts = pluralStringResource(
                R.plurals.feed_meta,
                element.commentCount,
                element.voteCount,
                element.commentCount,
            )
            "$counts  $typeLabel"
        } else {
            typeLabel
        }

        Text(
            text = metaLine,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}
