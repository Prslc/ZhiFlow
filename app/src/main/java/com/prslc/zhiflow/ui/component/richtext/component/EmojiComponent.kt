package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import coil3.compose.AsyncImage
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder
import com.prslc.zhiflow.ui.theme.TextStyles

/**
 * Builds the inline content for the emoji of [this] text, one image per emoji it carries.
 *
 * Both pairs come off the string's annotations rather than alongside the element, so a string
 * rebuilt for interception (`ZRichText`) still carries them, and every host that draws a parsed
 * text draws its emoji the same way. A placeholder with no entry here -- an emoji no host was
 * taught about -- falls back to its bracket, which is what the text showed before it had any.
 */
@Composable
fun AnnotatedString.rememberEmojiContent(): Map<String, InlineTextContent> {
    val paths = remember(this) {
        getStringAnnotations(AnnotatedStringBuilder.EMOJI_PATH_TAG, 0, length)
    }

    return remember(paths) {
        paths.mapNotNull { path ->
            val id = getStringAnnotations(AnnotatedStringBuilder.EMOJI_ID_TAG, path.start, path.end)
                .firstOrNull()?.item ?: return@mapNotNull null

            id to InlineTextContent(
                placeholder = Placeholder(
                    width = TextStyles.emojiSize,
                    height = TextStyles.emojiSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
                children = {
                    AsyncImage(
                        model = path.item,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                },
            )
        }.toMap()
    }
}
