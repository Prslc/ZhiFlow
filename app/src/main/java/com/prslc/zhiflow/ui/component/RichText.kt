package com.prslc.zhiflow.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.prslc.zhiflow.BuildConfig
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

                is RichTextElement.Heading -> {
                    Text(
                        text = element.content,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (element.level == 3) 20.sp else 22.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                is RichTextElement.Code -> {
                    CodeBlock(
                        code = element.code,
                        lang = element.lang
                    )
                }

                is RichTextElement.BulletItem -> {
                    BulletItemRow(
                        item = element,
                        uriHandler = uriHandler
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

                is RichTextElement.Blockquote -> {
                    BlockquoteComponent(
                        content = element.content,
                        uriHandler = uriHandler
                    )
                }

                is RichTextElement.Divider -> {
                    Divider()
                }

                else -> {
                    if (BuildConfig.DEBUG) {
                        Text(
                            text = "Unparsed: ${element::class.simpleName}",
                            color = Color.Red,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
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
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = content,
        style = style,
        onTextLayout = { layoutResult.value = it },
        modifier = modifier.pointerInput(Unit) {
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

@Composable
fun BulletItemRow(item: RichTextElement.BulletItem, uriHandler: UriHandler) {
    val indentation = (maxOf(0, item.level - 1) * 12).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentation, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = if (item.isOrdered) "${item.index}." else "•",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.widthIn(min = 16.dp)
        )

        ClickableText(
            content = item.content,
            onClick = { url -> uriHandler.openUri(url) },
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp)
        )
    }
}

@Composable
fun BlockquoteComponent(
    content: AnnotatedString,
    uriHandler: UriHandler
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(IntrinsicSize.Min)
    ) {
        Canvas(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .padding(vertical = 2.dp)
        ) {
            drawRoundRect(
                color = Color.LightGray.copy(alpha = 0.6f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }

        ClickableText(
            content = content,
            onClick = { url -> uriHandler.openUri(url) },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 24.sp
            ),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun CodeBlock(
    code: String,
    lang: String?
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Box(modifier = Modifier.horizontalScroll(scrollState)) {
                Text(
                    text = code,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    softWrap = false
                )
            }
        }
    }
}