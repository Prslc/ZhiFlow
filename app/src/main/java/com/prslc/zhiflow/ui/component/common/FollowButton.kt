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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
 * A follow toggle's behavior: the state it draws, the tap that flips it, the haptic that tap
 * plays, and the morph between the two states.
 *
 * The look is the caller's — [content] is handed the state to draw and the tap to call. Nothing
 * here holds the state: [isFollowing] is the caller's and this only draws it, which is why the tap
 * arrives as a plain callback.
 *
 * @param isFollowing Whether the reader follows the author. The toggle shows this state, it does
 *   not hold it.
 * @param onClick Called when the button is pressed, with the haptic for the new state already
 *   played.
 * @param modifier Applied to the animated content, so it survives the swap between the two states.
 * @param label Names the animation, so a trace of it says which toggle this was.
 * @param content What one state is drawn as: the state, and the tap that flips it.
 */
@Composable
fun FollowToggle(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "FollowToggle",
    content: @Composable (following: Boolean, onToggle: () -> Unit) -> Unit,
) {
    val toggleHaptic = rememberToggleHaptic()
    AnimatedContent(
        targetState = isFollowing,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(initialScale = 0.86f, animationSpec = tween(220)))
                .togetherWith(fadeOut(tween(90)) + scaleOut(targetScale = 0.92f, animationSpec = tween(90)))
                .using(SizeTransform(clip = false))
        },
        label = label,
    ) { following ->
        content(following) {
            toggleHaptic(!following)
            onClick()
        }
    }
}

/**
 * A follow toggle as a page draws it. `compact` sizes it down by capping the height from outside:
 * M3's Button enforces a 58x40dp minimum on its inner content row, and an outer `height()` is the
 * only way under that.
 *
 * @param isFollowing Whether the reader follows the author. The button shows this state, it does
 *   not hold it.
 * @param onClick Called when the button is pressed, with the haptic for the new state already
 *   played.
 * @param modifier Applied to the animated content, so it survives the swap between the two states.
 * @param compact True to size the button down for a card's header row.
 * @param followLabel What the button says while not yet followed. Defaults to the word for a
 *   person; a column is subscribed to rather than followed.
 * @param followingLabel What it says once it is.
 */
@Composable
fun FollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    followLabel: String = stringResource(R.string.people_action_follow),
    followingLabel: String = stringResource(R.string.people_action_following),
) {
    val contentPadding = if (compact) {
        PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    } else {
        ButtonDefaults.ContentPadding
    }
    val iconSize = if (compact) 14.dp else 18.dp
    val buttonModifier = if (compact) Modifier.height(30.dp) else Modifier

    FollowToggle(isFollowing, onClick, modifier, label = "FollowButton") { following, onToggle ->
        if (following) {
            FilledTonalButton(
                onClick = onToggle,
                modifier = buttonModifier,
                shape = CircleShape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                if (!compact) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(iconSize))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    followingLabel,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            Button(
                onClick = onToggle,
                modifier = buttonModifier,
                shape = CircleShape,
                contentPadding = contentPadding,
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(iconSize))
                Spacer(Modifier.width(4.dp))
                Text(
                    followLabel,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
