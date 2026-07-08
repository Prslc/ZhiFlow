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

data class ImageData(
    val url: String,
    val width: Int,
    val height: Int,
)

@Composable
fun ThumbnailRow(
    images: List<ImageData>,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 100.dp,
    onImageClick: (String) -> Unit = {},
) {
    if (images.isEmpty()) return

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
                        .clickable { onImageClick(image.url) },
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
