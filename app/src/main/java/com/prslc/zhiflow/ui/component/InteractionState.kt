package com.prslc.zhiflow.ui.component

import androidx.compose.runtime.Immutable

/**
 * What the reader's own relation to one piece of content is: the two votes, the favorite, and how
 * far the count under them has moved from the number the server sent.
 *
 * The content pages and the pin page hold one each -- the reader's relation to a pin is not the
 * one they have to the answer next to it -- but both take a vote the same way, so the type and the
 * one transition it takes are written here rather than twice.
 */
@Immutable
data class InteractionState(
    val isUpvoted: Boolean = false,
    val isDownvoted: Boolean = false,
    val isFavorite: Boolean = false,
    val upvoteOffset: Int = 0,
)

/** What a vote leaves behind: the state to draw, and whether it takes a vote back. */
internal data class VoteChange(
    val state: InteractionState,
    val isRevoke: Boolean,
)

/**
 * The vote [action] makes on this state, which the bar sends as `"up"` or `"down"`.
 *
 * [VoteChange.isRevoke] is read off the state the tap landed on, so it has to be taken before the
 * optimistic write: once the votes are the ones the reader just asked for, "is this a vote coming
 * off" answers the opposite of what the request needs. An action that is neither leaves the state
 * where it is.
 */
internal fun InteractionState.voteChange(action: String): VoteChange {
    val next = when (action) {
        "up" -> if (isUpvoted) {
            copy(isUpvoted = false, upvoteOffset = upvoteOffset - 1)
        } else {
            copy(isUpvoted = true, isDownvoted = false, upvoteOffset = upvoteOffset + 1)
        }

        "down" -> if (isDownvoted) {
            copy(isDownvoted = false)
        } else {
            copy(
                isDownvoted = true,
                isUpvoted = false,
                upvoteOffset = upvoteOffset - if (isUpvoted) 1 else 0,
            )
        }

        else -> this
    }

    return VoteChange(
        state = next,
        isRevoke = if (action == "up") isUpvoted else isDownvoted,
    )
}

/**
 * Puts back what a vote owns, and only that: the two votes, and the count they moved.
 *
 * The favorite is not a vote's to take back. The collection sheet writes it when its own request
 * lands, so restoring the whole state would undo a favorite confirmed in the meantime, leaving the
 * star off the bar while the collection holds the content.
 */
internal fun InteractionState.restoreVotes(from: InteractionState): InteractionState = copy(
    isUpvoted = from.isUpvoted,
    isDownvoted = from.isDownvoted,
    upvoteOffset = from.upvoteOffset,
)
