package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.data.model.content.Formula
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import kotlinx.serialization.json.Json

/**
 * Builds an inline formula placeholder sized by the server-provided dp dimensions.
 *
 * The Zhihu API reports each formula's display size in dp ([Formula.width]/[Formula.height],
 * the rendered bitmap is 3x that). The official app uses these exact dp values as the
 * span bounds, so formulas naturally vary in height (simple subscripts ~13dp, display
 * fractions up to ~56dp). [Placeholder] only accepts sp/em units, so the dp target is
 * converted via density and then divided by fontScale so the final rendered pixels stay
 * constant regardless of the user's system font size.
 *
 * @param widthDp server-reported width in dp
 * @param heightDp server-reported height in dp
 */
fun formulaPlaceholder(density: Density, widthDp: Float, heightDp: Float): Placeholder {
    val widthSp = with(density) { widthDp.dp.toPx().toSp() }
    val heightSp = with(density) { heightDp.dp.toPx().toSp() }
    return Placeholder(
        width = widthSp,
        height = heightSp,
        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
    )
}

/**
 * Returns the max inline formula width in dp, mirroring the official app: screen width
 * minus 2*21dp of horizontal padding.
 */
@Composable
internal fun rememberFormulaMaxWidth(): Float {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return (screenWidth - 2 * 21).coerceAtLeast(200).toFloat()
}

/**
 * Constrains a formula's dimensions to the content column width, mirroring the official
 * app: if the formula's width exceeds [maxWidthDp] (screen width minus horizontal
 * padding), scale both dimensions proportionally.
 */
internal fun constrainedSize(
    widthDp: Float,
    heightDp: Float,
    maxWidthDp: Float,
): Pair<Float, Float> {
    if (widthDp <= 0f || heightDp <= 0f) return widthDp to heightDp
    if (widthDp <= maxWidthDp) return widthDp to heightDp
    val scale = maxWidthDp / widthDp
    return maxWidthDp to (heightDp * scale)
}

@Composable
fun LatexComponent(
    formula: Formula,
    modifier: Modifier = Modifier,
    isInline: Boolean = false,
    maxWidthDp: Float = -1f,
) {
    val effectiveMaxWidth = if (maxWidthDp > 0f) maxWidthDp else rememberFormulaMaxWidth()
    if (isInline) {
        val (widthDp, heightDp) = constrainedSize(
            formula.width.toFloat(), formula.height.toFloat(), effectiveMaxWidth
        )
        FormulaImage(
            formula = formula,
            modifier = modifier
                .width(widthDp.dp)
                .height(heightDp.dp),
        )
    } else {
        val (widthDp, heightDp) = constrainedSize(
            formula.width.toFloat(), formula.height.toFloat(), effectiveMaxWidth
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
        ) {
            FormulaImage(
                formula = formula,
                modifier = Modifier
                    .width(widthDp.dp)
                    .height(heightDp.dp),
            )
        }
    }
}

@Composable
private fun FormulaImage(
    formula: Formula,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = formula.imgUrl,
        contentDescription = formula.content,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
        colorFilter = if (isSystemInDarkTheme()) {
            // Mirror the official app: invert the white-background formula bitmap so the
            // background turns dark and the black glyphs turn light in dark mode.
            val matrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
            ColorFilter.colorMatrix(matrix)
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
    val density = LocalDensity.current
    val maxWidthDp = rememberFormulaMaxWidth()

    val inlineContentMap = remember(element.inlineMetas, density, maxWidthDp) {
        element.inlineMetas.associate { meta ->
            val (widthDp, heightDp) = constrainedSize(
                meta.formula.width.toFloat(), meta.formula.height.toFloat(), maxWidthDp
            )
            meta.inlineId to InlineTextContent(formulaPlaceholder(density, widthDp, heightDp)) {
                LatexComponent(
                    formula = meta.formula,
                    isInline = true,
                    modifier = Modifier.fillMaxSize(),
                    maxWidthDp = maxWidthDp,
                )
            }
        }
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    // Compose does not grow a text row to fit a tall inline placeholder (unlike the official
    // app's getSize font-metrics adjustment), so tall formulas (\displaystyle fractions) would
    // overlap adjacent rows. Raise the paragraph line height to cover the tallest formula.
    val maxFormulaHeightDp = element.inlineMetas.maxOfOrNull {
        constrainedSize(it.formula.width.toFloat(), it.formula.height.toFloat(), maxWidthDp).second
    } ?: 0f
    val baseLineHeight = MaterialTheme.typography.bodyLarge.lineHeight
    val effectiveLineHeight = if (maxFormulaHeightDp > 0f) {
        val formulaSp = with(density) { (maxFormulaHeightDp + 8f).dp.toSp() }
        if (formulaSp.value > baseLineHeight.value) formulaSp else baseLineHeight
    } else {
        baseLineHeight
    }

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
            lineHeight = effectiveLineHeight,
            letterSpacing = 0.25.sp,
        )
    )
}
