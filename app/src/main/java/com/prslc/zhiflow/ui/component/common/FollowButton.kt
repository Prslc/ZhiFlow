package com.prslc.zhiflow.ui.component.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R

@Composable
fun FollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val contentPadding = if (compact) {
        PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    } else {
        ButtonDefaults.ContentPadding
    }
    val iconSize = if (compact) 14.dp else 18.dp
    // M3 Button enforces a 58x40dp minimum on its inner content row; an outer
    // height() cap is the only way to shrink it below that.
    val buttonModifier = if (compact) Modifier.height(30.dp) else Modifier
    AnimatedContent(
        targetState = isFollowing,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(initialScale = 0.86f, animationSpec = tween(220)))
                .togetherWith(fadeOut(tween(90)) + scaleOut(targetScale = 0.92f, animationSpec = tween(90)))
                .using(SizeTransform(clip = false))
        },
        label = "FollowButton"
    ) { following ->
        if (following) {
            FilledTonalButton(
                onClick = onClick,
                modifier = buttonModifier,
                shape = CircleShape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (!compact) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(iconSize))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    stringResource(R.string.people_action_following),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        } else {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                shape = CircleShape,
                contentPadding = contentPadding
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(iconSize))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.people_action_follow),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
