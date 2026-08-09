package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R

@Composable
fun ContentTypeLabel(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold,
        )
    }
}

data class ContentTypeConfig(
    val labelResId: Int,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
fun contentTypeConfig(type: String?): ContentTypeConfig = when (type) {
    "answer" -> ContentTypeConfig(
        labelResId = R.string.type_answer,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
    )
    "article" -> {
        val scheme = MaterialTheme.colorScheme
        val content = scheme.primary.shiftHue(120f)
        val container = lerp(scheme.surface, content, if (isSystemInDarkTheme()) 0.25f else 0.12f)
        ContentTypeConfig(
            labelResId = R.string.type_article,
            containerColor = container,
            contentColor = content,
        )
    }
    "pin" -> {
        val scheme = MaterialTheme.colorScheme
        val content = scheme.primary.shiftHue(240f)
        val container = lerp(scheme.surface, content, if (isSystemInDarkTheme()) 0.25f else 0.12f)
        ContentTypeConfig(
            labelResId = R.string.type_thought,
            containerColor = container,
            contentColor = content,
        )
    }
    else -> ContentTypeConfig(
        labelResId = R.string.type_unknown,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun Color.shiftHue(degrees: Float): Color {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    val s = if (d == 0f) 0f else d / (1f - kotlin.math.abs(2f * l - 1f))
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Color.hsl((h + degrees) % 360f, s, l, alpha)
}
