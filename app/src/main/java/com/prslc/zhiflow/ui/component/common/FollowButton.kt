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
import com.prslc.zhiflow.core.utils.compose.rememberToggleHaptic

/**
 * A follow toggle. `compact` sizes it down by capping the height from outside: M3's Button enforces a
 * 58x40dp minimum on its inner content row, and an outer `height()` is the only way under that.
 */
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
    val buttonModifier = if (compact) Modifier.height(30.dp) else Modifier
    val toggleHaptic = rememberToggleHaptic()
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
        val onToggle = {
            toggleHaptic(!following)
            onClick()
        }
        if (following) {
            FilledTonalButton(
                onClick = onToggle,
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
                onClick = onToggle,
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
