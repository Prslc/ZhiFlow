package com.prslc.zhiflow.ui.component.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R

/**
 * An avatar and a name, with room after them for whatever the caller's row needs.
 *
 * A blank [authorName] is drawn as the anonymous-user string, so a caller may pass an absent name
 * straight through.
 *
 * @param avatarUrl The avatar to load, or null to leave Coil's placeholder.
 * @param authorName The name to draw; blank becomes the anonymous-user string.
 * @param modifier Applied to the `Row`, so a caller's padding and arrangement reach the avatar and
 *   the trailing slot too.
 * @param avatarSize The avatar's diameter; the name is not sized from it.
 * @param nameStyle The style the name is drawn with. [fontWeight] overrides the weight it carries.
 * @param nameColor The colour of the name; the avatar is not tinted.
 * @param fontWeight The name's weight, or null to take the one in [nameStyle].
 * @param trailing Content drawn after the name, for a trailing action.
 */
@Composable
fun AuthorRow(
    avatarUrl: String?,
    authorName: String,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 24.dp,
    nameStyle: TextStyle = MaterialTheme.typography.labelLarge,
    nameColor: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = stringResource(R.string.avatar_desc),
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = authorName.ifEmpty { stringResource(R.string.anonymous_user) },
            style = nameStyle,
            color = nameColor,
            fontWeight = fontWeight,
        )

        trailing()
    }
}
