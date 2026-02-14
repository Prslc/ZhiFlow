package com.prslc.zhiflow.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prslc.zhiflow.data.model.Segment
import com.prslc.zhiflow.data.model.ZhihuImage
import com.prslc.zhiflow.parser.ContentParser
import com.prslc.zhiflow.parser.RichTextElement

@Composable
fun RichText(
    segments: List<Segment>,
    onImageClick: (String) -> Unit
) {
    val elements = remember(segments) {
        ContentParser.transform(segments)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        elements.forEach { element ->
            when (element) {
                is RichTextElement.Text -> {
                    Text(
                        text = element.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 32.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                is RichTextElement.Image -> {
                    ImageComponent(
                        image = element.data,
                        onImageClick = onImageClick
                    )
                }

                is RichTextElement.Divider -> {
                    Divider()
                }
            }
        }
    }
}

@Composable
fun ImageComponent(
    image: ZhihuImage?,
    onImageClick: (String) -> Unit
) {
    val imgUrl = image?.urls?.firstOrNull() ?: return

    Card(
        modifier = Modifier.clickable { onImageClick(imgUrl) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imgUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}