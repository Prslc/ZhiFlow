package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prslc.zhiflow.parser.RichTextElement
import com.prslc.zhiflow.ui.navigation.LocalNavigator

@Composable
fun RichTextComponent(
    content: AnnotatedString,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FormulaTextSection(
        content = content,
        onImageClick = onImageClick,
        modifier = modifier
    )
}

@Composable
fun Heading(element: RichTextElement.Heading) {
    val navigator = LocalNavigator.current

    ClickableText(
        content = element.content,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (element.level == 3) 20.sp else 22.sp
        ),
        modifier = Modifier.padding(top = 8.dp),
        onClick = { url ->
            navigator.handleUrl(url)
        }
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