package com.prslc.zhiflow.ui.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a visit writes down and reads back.
 *
 * A saved stack is a list of visits, each written as its class name plus its own form, and the
 * route inside a visit is written the same way again. Two visits to one route have to come back as
 * two visits: navigation keys an entry's saved state and ViewModel by the visit's id, so a
 * collision is two pages sharing a scroll position and a ViewModel.
 */
class NavVisitTest {

    private fun roundTrip(visit: NavVisit): NavVisit =
        Json.decodeFromString(NavVisit.serializer(), Json.encodeToString(NavVisit.serializer(), visit))

    @Test
    fun `a data route survives a visit's round trip`() {
        val restored = roundTrip(NavVisit(id = 7, route = QuestionDetail("123")))

        assertEquals(7L, restored.id)
        assertEquals(QuestionDetail("123"), restored.route)
    }

    @Test
    fun `an object route survives a visit's round trip`() {
        val restored = roundTrip(NavVisit(id = 8, route = Settings))

        assertEquals(8L, restored.id)
        assertTrue(restored.route is Settings)
    }

    @Test
    fun `a route with a defaulted field survives`() {
        val restored = roundTrip(NavVisit(id = 9, route = Chat("peer")))

        assertEquals(Chat(peerId = "peer", sourceType = null), restored.route)
    }

    @Test
    fun `two visits to one route come back apart`() {
        val first = roundTrip(NavVisit(id = 1, route = AnswerDetail("a")))
        val second = roundTrip(NavVisit(id = 2, route = AnswerDetail("a")))

        assertEquals(first.route, second.route)
        assertNotEquals(first.id, second.id)
    }
}
