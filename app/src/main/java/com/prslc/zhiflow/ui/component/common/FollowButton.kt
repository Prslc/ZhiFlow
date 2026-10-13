package com.prslc.zhiflow.ui.component.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface

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

/**
 * The follow button an author's own row wears: a wash of the primary color with that color written
 * on it, and a plus.
 *
 * The wash is this app's own rather than a role the scheme names -- about an eighth of the way from
 * the surface to the primary. It reads by its words rather than by its fill, which is what keeps it
 * legible under the dynamic palette, where a container and its surface can sit within a few steps
 * of each other.
 *
 * Its size is capped from the outside: M3's Button enforces 58x40dp minimums on its inner content
 * row, and such a default only fills in where the incoming minimum is zero -- so a height of its
 * own is what gets under it.
 *
 * @param isFollowing Whether the reader follows the author. The button shows this state, it does
 *   not hold it.
 * @param onClick Called when the button is pressed, with the haptic for the new state already
 *   played.
 * @param modifier Applied to the animated content, so it survives the swap between the two states.
 * @param compact True for the size a page header wears it at, where the name it sits beside is the
 *   page's subject. Left false it is a row's: 30dp, which is what an author's row had before this
 *   button moved here, and most of the height of the avatar beside it.
 */
@Composable
fun AuthorFollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val followLabel = stringResource(R.string.people_action_follow)
    val followingLabel = stringResource(R.string.people_action_following)
    val buttonModifier = Modifier
        .height(if (compact) 22.dp else 30.dp)
        .widthIn(min = 44.dp)
    FollowToggle(
        isFollowing = isFollowing,
        onClick = onClick,
        modifier = modifier,
        label = "AuthorFollowButton",
    ) { following, onToggle ->
        Button(
            onClick = onToggle,
            modifier = buttonModifier,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (following) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                },
                contentColor = if (following) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Icon(
                imageVector = if (following) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = if (following) followingLabel else followLabel,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@AppPreviews
@Composable
private fun FollowButtonPreview() {
    PreviewSurface {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FollowButton(isFollowing = false, onClick = {})
            FollowButton(isFollowing = true, onClick = {})
        }
    }
}

/** The size a card's header row uses, which is the same pair capped to 30dp. */
@AppPreviews
@Composable
private fun FollowButtonCompactPreview() {
    PreviewSurface {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FollowButton(isFollowing = false, onClick = {}, compact = true)
            FollowButton(isFollowing = true, onClick = {}, compact = true)
        }
    }
}

@AppPreviews
@Composable
private fun AuthorFollowButtonPreview() {
    PreviewSurface {
        Row {
            AuthorFollowButton(isFollowing = false, onClick = {})
            AuthorFollowButton(isFollowing = true, onClick = {})
        }
    }
}

/** The same two, at the size a page header wears it: the name beside it is the page's subject. */
@AppPreviews
@Composable
private fun AuthorFollowButtonCompactPreview() {
    PreviewSurface {
        Row {
            AuthorFollowButton(isFollowing = false, onClick = {}, compact = true)
            AuthorFollowButton(isFollowing = true, onClick = {}, compact = true)
        }
    }
}
