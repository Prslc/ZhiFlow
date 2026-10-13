package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import com.prslc.zhiflow.ui.theme.LocalIsDarkTheme

/**
 * The badge a card wears to say what it is.
 *
 * It is drawn in one of two forms, and which one is a question of how loudly it should speak.
 * Filled, it carries its color in a wash and reads at a glance; outlined, the same words in the
 * same color are held by a border on the surface instead, which sits lighter on the row -- for a
 * marker that is meant to be read rather than noticed.
 *
 * @param text The label to draw; callers take it from [contentTypeConfig].
 * @param modifier Applied to the `Surface` the badge is drawn on, so a caller's padding lands
 *   outside the badge's own.
 * @param containerColor The badge's background; [contentTypeConfig] supplies the pair it
 *   belongs to. Left unread when [outlined], which fills nothing.
 * @param contentColor What the label is drawn in. The surface carries it, so the text inherits it,
 *   and the outline is drawn in it too: the border and the words are one color, as they are in the
 *   filled form, where the wash is the other half of the pair.
 * @param outlined True to hold the badge's shape with a border rather than a wash.
 */
@Composable
fun BadgeLabel(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    outlined: Boolean = false,
) {
    Surface(
        color = if (outlined) Color.Transparent else containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
        border = if (outlined) BorderStroke(1.dp, contentColor) else null,
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
 * The label and colors a content type is drawn with.
 *
 * The article and pin badges take their hue from the primary one rather than naming a color of
 * their own, so they follow the scheme the reader is on.
 *
 * @param type The API's content-type string: `answer`, `article` or `pin`, in whatever case it
 *   comes in. Anything else, including null, gets the unknown badge.
 */
@Composable
@ReadOnlyComposable
fun contentTypeConfig(type: String?): ContentTypeConfig = when (type?.lowercase()) {
    "answer" -> {
        ContentTypeConfig(
            labelResId = R.string.type_answer,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        )
    }

    "article" -> {
        val scheme = MaterialTheme.colorScheme
        val content = scheme.primary.shiftHue(120f)
        val container = lerp(scheme.surface, content, if (LocalIsDarkTheme.current) 0.25f else 0.12f)
        ContentTypeConfig(
            labelResId = R.string.type_article,
            containerColor = container,
            contentColor = content,
        )
    }

    "pin" -> {
        val scheme = MaterialTheme.colorScheme
        val content = scheme.primary.shiftHue(240f)
        val container = lerp(scheme.surface, content, if (LocalIsDarkTheme.current) 0.25f else 0.12f)
        ContentTypeConfig(
            labelResId = R.string.type_thought,
            containerColor = container,
            contentColor = content,
        )
    }

    else -> {
        ContentTypeConfig(
            labelResId = R.string.type_unknown,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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

/** Every badge the app can wear, in both forms, the one a type it does not know gets included. */
@AppPreviews
@Composable
private fun BadgeLabelPreview() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The same four twice: once with the wash carrying the shape, once with a border.
            listOf(false, true).forEach { outlined ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("answer", "article", "pin", "question").forEach { type ->
                        val config = contentTypeConfig(type)
                        BadgeLabel(
                            text = stringResource(config.labelResId),
                            containerColor = config.containerColor,
                            contentColor = config.contentColor,
                            outlined = outlined,
                        )
                    }
                }
            }
        }
    }
}
