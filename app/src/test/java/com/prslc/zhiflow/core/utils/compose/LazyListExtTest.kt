package com.prslc.zhiflow.core.utils.compose

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Taking a page into a list: the one place a paged state is built, and the three answers it owes.
 *
 * The rows of a paged list are drawn under a key taken from the row itself, and a key measured
 * twice throws where it is drawn. A row stands in for itself here -- the key of a `String` list is
 * the string -- so each test says one thing about what the caller gets back.
 */
class LazyListExtTest {

    @Test
    fun `a page after leaves what was held, and then what it adds`() {
        val held = listOf("a", "b")

        assertEquals(
            listOf("a", "b", "c", "d"),
            held.withPage(listOf("c", "d"), firstPage = false) { it },
        )
    }

    @Test
    fun `a row the list already holds, or the page repeats, is not added twice`() {
        val held = listOf("a", "b")

        assertEquals(
            listOf("a", "b", "c"),
            held.withPage(listOf("b", "a", "c", "c"), firstPage = false) { it },
        )
    }

    @Test
    fun `a first page leaves the page alone, and does not consult what was held`() {
        // What the list held is off screen once a page replaces it: a row the page carries again
        // is not a repeat, and is not dropped against a list nobody will see.
        val held = listOf("x", "y")

        assertEquals(
            listOf("a", "b"),
            held.withPage(listOf("a", "a", "b"), firstPage = true) { it },
        )
    }
}
