package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.data.model.Formula
import com.prslc.zhiflow.parser.RichTextElement
import kotlinx.serialization.json.Json

@Composable
fun RichTextComponent(
    content: AnnotatedString,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val inlineContentMap = remember(content) {
        val map = mutableMapOf<String, InlineTextContent>()
        content.getStringAnnotations("INLINE_FORMULA_DATA", 0, content.length)
            .forEach { annotation ->
                val formula = Json.decodeFromString<Formula>(annotation.item)
                val inlineId = "f_${annotation.start}"
                map[inlineId] = InlineTextContent(
                    placeholder = Placeholder(
                        width = (20 * (formula.width.toFloat() / formula.height.coerceAtLeast(1))).sp,
                        height = 20.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    ),
                    children = {
                        LatexComponent(
                            formula = formula,
                            isInline = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            }
        map
    }

    Text(
        text = content,
        modifier = modifier.pointerInput(content) {
            detectTapGestures { pos ->
                layoutResult.value?.let { layout ->
                    val offset = layout.getOffsetForPosition(pos)

                    // click link
                    content.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }

                    // inline formula
                    content.getStringAnnotations("INLINE_FORMULA_DATA", offset, offset)
                        .firstOrNull()?.let { annotation ->
                            runCatching { Json.decodeFromString<Formula>(annotation.item) }
                                .getOrNull()?.imgUrl?.let { onImageClick(it) }
                        }
                }
            }
        },
        inlineContent = inlineContentMap,
        onTextLayout = { layoutResult.value = it },
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = 26.sp,
            letterSpacing = 0.25.sp
        )
    )
}

@Composable
fun Heading(element: RichTextElement.Heading) {
    Text(
        text = element.content,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = if (element.level == 3) 20.sp else 22.sp
        ),
        modifier = Modifier.padding(top = 8.dp)
    )
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