package com.prslc.zhiflow.core.preference

import kotlinx.coroutines.flow.StateFlow

/**
 * Where the phrases the reader has searched for are kept.
 *
 * Zhihu serves no endpoint that reads a history back: the official client keeps its own and sends
 * it up as `local_queries` when it reports a search. This is that list, kept on the device and
 * offered by the search box before anything has been typed.
 *
 * Reads publish, so a phrase recorded or a list cleared reaches the box that is drawing it.
 */
interface SearchHistoryStore {

    /** The phrases, most recently searched first. */
    val queries: StateFlow<List<String>>

    /**
     * Puts a phrase at the front of the list, moving it there if it was already in it.
     *
     * Anything past the last kept phrase is dropped.
     *
     * @param query The phrase that was searched for. Blank is not history, and is ignored.
     */
    fun record(query: String)

    /**
     * Drops one phrase from the list.
     *
     * A phrase that is not in it is a no-op.
     *
     * @param query The phrase to drop.
     */
    fun remove(query: String)

    /** Empties the list, which is what the search box then has to show in its place. */
    fun clear()
}
