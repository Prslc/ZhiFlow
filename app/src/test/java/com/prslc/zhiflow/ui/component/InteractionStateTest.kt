package com.prslc.zhiflow.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What one tap on the bar leaves behind, and what a failed vote puts back.
 *
 * Both are pure, so what the reader sees after a tap is decided here rather than watched on a
 * device. The count under the two votes is the number the server sent plus the moves made since,
 * and a rollback reaches over the part of the state a vote owns.
 */
class InteractionStateTest {

    @Test
    fun `a vote goes on and the count moves with it`() {
        val change = InteractionState().voteChange("up")

        assertEquals(InteractionState(isUpvoted = true, upvoteOffset = 1), change.state)
        assertFalse(change.isRevoke)
    }

    @Test
    fun `voting the other way takes the first vote off`() {
        val down = InteractionState().voteChange("down")

        assertEquals(InteractionState(isDownvoted = true), down.state)

        val change = down.state.voteChange("up")

        assertEquals(InteractionState(isUpvoted = true, upvoteOffset = 1), change.state)
    }

    @Test
    fun `the same vote twice takes it back`() {
        val up = InteractionState().voteChange("up")

        val change = up.state.voteChange("up")

        assertEquals(InteractionState(), change.state)
        assertTrue(change.isRevoke)
    }

    @Test
    fun `a vote coming off is read off the state the tap landed on`() {
        // The request needs the state before the write: taken after it, the same tap would tell
        // the server to vote rather than to take the vote back.
        val change = InteractionState(isUpvoted = true).voteChange("up")

        assertFalse(change.state.isUpvoted)
        assertTrue(change.isRevoke)
    }

    @Test
    fun `a failed vote comes back to where it started, not to the state it left`() {
        // Upvoted, then a down vote that failed. Taking the down off again is not the answer: it
        // would leave the reader with no vote at all, where the tap found one.
        val start = InteractionState(isUpvoted = true, upvoteOffset = 1)

        val afterDown = start.voteChange("down").state

        assertEquals(start, afterDown.restoreVotes(start))
    }

    @Test
    fun `a failed vote leaves a favorite the sheet confirmed where it is`() {
        val was = InteractionState(isUpvoted = true, upvoteOffset = 1)
        val afterDown = was.voteChange("down").state

        val current = afterDown.copy(isFavorite = true)

        val restored = current.restoreVotes(was)

        assertTrue(restored.isFavorite)
        assertEquals(was.isUpvoted, restored.isUpvoted)
        assertEquals(was.isDownvoted, restored.isDownvoted)
        assertEquals(was.upvoteOffset, restored.upvoteOffset)
    }

    @Test
    fun `an action the bar never sends leaves the state where it is`() {
        val state = InteractionState(isDownvoted = true)

        assertEquals(state, state.voteChange("sideways").state)
    }
}
