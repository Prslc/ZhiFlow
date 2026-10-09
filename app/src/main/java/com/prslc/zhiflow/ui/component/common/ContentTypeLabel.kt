package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R

/**
 * The badge a card wears to say what it is.
 *
 * @param text The label to draw; callers take it from [contentTypeConfig].
 * @param modifier Applied to the `Surface` the badge is drawn on, so a caller's padding lands
 *   outside the badge's own.
 * @param containerColor The badge's background; [contentTypeConfig] supplies the pair it
 *   belongs to.
 * @param contentColor What the label is drawn in. The surface carries it, so the text inherits it.
 */
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

/**
 * The label and colours a content type is drawn with.
 *
 * The article and pin badges take their hue from the primary one rather than naming a colour of
 * their own, so they follow the scheme the reader is on.
 *
 * @param type The API's content-type string: `answer`, `article` or `pin`, in whatever case it
 *   comes in. Anything else, including null, gets the unknown badge.
 */
@Composable
@ReadOnlyComposable
fun contentTypeConfig(type: String?): ContentTypeConfig = when (type?.lowercase()) {
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
    val l = ((max + min) / 2f).coerceIn(0f, 1f)
    val d = max - min
    val denom = 1f - kotlin.math.abs(2f * l - 1f)
    val s = if (d == 0f || denom == 0f) 0f else (d / denom).coerceIn(0f, 1f)
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Color.hsl((h + degrees) % 360f, s, l, alpha)
}
