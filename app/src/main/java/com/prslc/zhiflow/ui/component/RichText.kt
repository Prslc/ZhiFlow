package com.prslc.zhiflow.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.prslc.zhiflow.R
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

    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        elements.forEach { element ->
            when (element) {
                is RichTextElement.Text -> {
                    ClickableText(
                        content = element.content,
                        onClick = { url ->
                            uriHandler.openUri(url)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp,
                            letterSpacing = 0.25.sp
                        ),
                    )
                }

                is RichTextElement.Image -> {
                    ImageComponent(
                        image = element.data,
                        onImageClick = onImageClick
                    )
                }

                is RichTextElement.Reference -> {
                    ReferenceSection(element.items)
                }

                is RichTextElement.Divider -> {
                    Divider()
                }

                else -> {}
            }
        }
    }
}

@Composable
fun ImageComponent(
    image: ZhihuImage?,
    onImageClick: (String) -> Unit
) {
    val displayUrl = remember(image) {
        if (image?.isGif == true) {
            image.urls.find { it.contains(".gif", ignoreCase = true) } ?: image.urls.firstOrNull()
        } else {
            image?.urls?.firstOrNull()
        }
    } ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Card(
            modifier = Modifier.clickable { onImageClick(displayUrl) },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(displayUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
        if (!image?.description.isNullOrBlank()) {
            Text(
                text = image.description,
                modifier = Modifier
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
            )
        }
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

@Composable
fun ReferenceSection(items: List<AnnotatedString>) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.richtext_references),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        items.forEachIndexed { index, content ->
            val fullAnnotatedString = remember(content) {
                buildAnnotatedString {
                    append("${index + 1}. ")
                    append(content)
                }
            }

            ClickableText(
                content = fullAnnotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 20.sp,
                    color = Color.Gray
                ),
                onClick = { url ->
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        throw e
                    }
                }
            )
        }
    }
}


@Composable
fun ClickableText(
    content: AnnotatedString,
    style: TextStyle,
    onClick: (String) -> Unit
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = content,
        style = style,
        onTextLayout = { layoutResult.value = it },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { pos ->
                layoutResult.value?.let { layout ->
                    val offset = layout.getOffsetForPosition(pos)
                    content.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            onClick(annotation.item)
                        }

                    content.getStringAnnotations(tag = "FORMULA", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            onClick(annotation.item)
                        }
                }
            }
        }
    )
}