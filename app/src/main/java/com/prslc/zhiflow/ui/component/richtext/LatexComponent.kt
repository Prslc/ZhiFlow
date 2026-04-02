package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.data.model.Formula

@Composable
fun LatexComponent(
    formula: Formula,
    modifier: Modifier = Modifier,
    isInline: Boolean = false,
) {
    val aspectRatio = formula.width.toFloat() / formula.height.toFloat()
    val density = LocalDensity.current

    val targetHeight = if (isInline) 20.dp else with(density) { (formula.height * 2.5f).toDp() }

    if (isInline) {
        AsyncImage(
            model = formula.imgUrl,
            contentDescription = formula.content,
            modifier = modifier.height(targetHeight).aspectRatio(aspectRatio),
            contentScale = ContentScale.Fit,
            colorFilter = if (isSystemInDarkTheme()) ColorFilter.tint(Color.White, BlendMode.SrcIn) else null
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AsyncImage(
                    model = formula.imgUrl,
                    contentDescription = formula.content,
                    modifier = modifier
                        .height(targetHeight)
                        .aspectRatio(aspectRatio),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isSystemInDarkTheme()) ColorFilter.tint(Color.White, BlendMode.SrcIn) else null
                )
            }
        }
    }
}