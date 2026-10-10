package com.prslc.zhiflow.core.network

import okhttp3.Request

/** Marks a request that establishes a session rather than using one. */
private object SessionEstablishing

/**
 * The funnel's line to the session.
 *
 * Every call meets it twice: before the request goes out, and when the answer comes back.
 */
interface SessionGate {

    /**
     * Suspends until a session is available to send.
     *
     * The first call after a cold start can take a round trip: nothing is stored yet, and an
     * anonymous session has to be fetched before anything can go out. It returns anyway when
     * that fetch fails — a request with no credentials is better than a request that never
     * happens, and the caller's own error handling is what the reader sees.
     */
    suspend fun awaitReady()

    /**
     * Offers an exchange's outcome.
     *
     * Neither suspends nor throws: answering a request is not a place to make the caller wait on
     * session bookkeeping. Anything it decides to do happens on its own scope.
     *
     * @param status The HTTP status the server answered with.
     */
    fun onExchange(status: Int)
}

/**
 * Marks this request as one that establishes a session, so the funnel does not wait for one
 * first.
 *
 * Only the guest bootstrap needs it: waiting there would be waiting on the call that would
 * satisfy the wait, while holding the lock that call has to take.
 */
internal fun Request.Builder.establishesSession(): Request.Builder =
    tag(SessionEstablishing::class.java, SessionEstablishing)

/** True when this request establishes a session, and so must go out without waiting for one. */
internal fun Request.establishesSession(): Boolean =
    tag(SessionEstablishing::class.java) != null
