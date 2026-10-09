package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.ui.component.widget.ImageLightboxController
import com.prslc.zhiflow.ui.component.widget.LightboxItem
import org.koin.compose.koinInject

/**
 * One image and the box it is drawn in.
 *
 * [width] and [height] carry that box's proportions, not a pixel size: a thumbnail is drawn at
 * their ratio, and a feed card's images already carry the ratio its own style asks for.
 */
data class ImageData(
    val url: String,
    val width: Int,
    val height: Int,
)

/** Display-layer adapter for the lightbox: a thumbnail only carries a single url. */
private fun ImageData.toZhihuImage(): ZhihuImage = ZhihuImage(
    urls = listOf(url),
    width = width,
    height = height,
    description = "",
    isGif = false,
)

/**
 * A row of tappable thumbnails. The lightbox is opened with a list kept 1:1 with [images]: the
 * click handler indexes into it and the controller matches by identity.
 *
 * @param images The thumbnails to draw, in order. An empty list draws nothing at all.
 * @param modifier Applied to the scrolling `Row`, so the horizontal scroll belongs to it; a caller
 *   does not wrap it for that.
 * @param imageHeight The height each thumbnail is drawn at; the width follows the image's own
 *   ratio, capped at 150dp.
 */
@Composable
fun ThumbnailRow(
    images: List<ImageData>,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 100.dp,
) {
    if (images.isEmpty()) return

    val lightbox = koinInject<ImageLightboxController>()
    val lightboxItems = remember(images) { images.map { LightboxItem.Image(it.toZhihuImage()) } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        images.forEachIndexed { index, image ->
            key("${image.url}_$index") {
                val aspectRatio = remember(image) {
                    if (image.width > 0 && image.height > 0)
                        image.width.toFloat() / image.height.toFloat()
                    else 1f
                }

                AsyncImage(
                    model = image.url,
                    contentDescription = null,
                    modifier = Modifier
                        .height(imageHeight)
                        .widthIn(max = 150.dp)
                        .aspectRatio(aspectRatio)
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            if (image.url.isBlank()) return@clickable
                            val tapped = lightboxItems.getOrNull(index)?.image
                                ?: return@clickable
                            lightbox.open(lightboxItems, tapped)
                        },
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
