package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.data.model.content.Formula
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import kotlinx.serialization.json.Json

private val InlineFormulaHeightDp = 22.dp
private val InlineFormulaHeightSp = 22.sp
@Composable
fun LatexComponent(
    formula: Formula,
    modifier: Modifier = Modifier,
    isInline: Boolean = false,
) {
    if (isInline) {
        FormulaImage(
            formula = formula,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FormulaImage(formula = formula)
            }
        }
    }
}

@Composable
private fun FormulaImage(
    formula: Formula,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = if (formula.width > 0 && formula.height > 0) {
        formula.width.toFloat() / formula.height.toFloat()
    } else {
        1f
    }

    AsyncImage(
        model = formula.imgUrl,
        contentDescription = formula.content,
        modifier = modifier
            .height(InlineFormulaHeightDp)
            .aspectRatio(aspectRatio),
        contentScale = ContentScale.Fit,
        colorFilter = if (isSystemInDarkTheme()) {
            ColorFilter.tint(Color.White, BlendMode.SrcIn)
        } else {
            null
        }
    )
}

@Composable
fun FormulaTextSection(
    element: RichTextElement.ParsedText,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current

    val inlineContentMap = remember(element.inlineMetas) {
        element.inlineMetas.associate { meta ->
            meta.inlineId to InlineTextContent(meta.formula.placeholder()) {
                LatexComponent(
                    formula = meta.formula,
                    isInline = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = element.content,
        modifier = modifier.pointerInput(element.content) {
            detectTapGestures { pos ->
                layoutResult.value?.let { layout ->
                    val offset = layout.getOffsetForPosition(pos)
                    element.content.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { navigator.handleUrl(it.item) }

                    element.content.getStringAnnotations("INLINE_FORMULA_DATA", offset, offset)
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
            letterSpacing = 0.25.sp,
        )
    )
}

private fun Formula.placeholder(): Placeholder {
    val aspectRatio = if (width > 0 && height > 0) {
        width.toFloat() / height.toFloat()
    } else {
        1f
    }
    return Placeholder(
        width = InlineFormulaHeightSp * aspectRatio,
        height = InlineFormulaHeightSp,
        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
    )
}
