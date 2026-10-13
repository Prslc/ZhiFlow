package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface

/**
 * A column's picture, or what this client draws when it has none.
 *
 * Plenty of columns have never had a picture, and the API says so with an empty string rather than
 * with a default: the address a column's own page reads carries none, while the same column in a
 * feed is drawn with the server's own stand-in. Left alone, such a column is an empty square beside
 * its name.
 *
 * The stand-in here is this client's rather than the server's: the field and the mark on it are the
 * theme's own colors, so it follows the palette and the mode the reader is in.
 *
 * @param url The picture's address, or null when the payload carried neither one nor the other.
 * @param modifier Applied to whichever of the two is drawn. The caller sizes and clips it, since
 *   the two call sites round their corners by different amounts.
 */
@Composable
fun ColumnCover(url: String?, modifier: Modifier = Modifier) {
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
        return
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Article,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(COVER_MARK_FRACTION),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** How much of the square the mark takes, leaving the rest of it tone around the mark. */
private const val COVER_MARK_FRACTION = 0.5f

/** Both sizes the app draws a column at: the card's, and the page header's. */
@AppPreviews
@Composable
private fun ColumnCoverPreview() {
    PreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColumnCover(
                url = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            ColumnCover(
                url = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}
