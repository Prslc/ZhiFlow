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
