package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.ui.theme.TextStyles

@Composable
fun ImageComponent(
    image: ZhihuImage?,
    onImageClick: (ZhihuImage) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (image == null) return
    val displayUrl = image.displayUrl ?: return

    val aspectRatio = remember(image) {
        if (image.width > 0 && image.height > 0) image.width.toFloat() / image.height.toFloat()
        else 1.77f    // 16:9
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clickable { onImageClick(image) },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(displayUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        }
        if (image.description.isNotBlank()) {
            Text(
                text = image.description,
                modifier = Modifier
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = TextStyles.imageCaptionSize,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}
