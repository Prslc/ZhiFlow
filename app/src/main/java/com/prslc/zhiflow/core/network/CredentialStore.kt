package com.prslc.zhiflow.core.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Where the session and the device id are kept.
 *
 * A port so that `core` can read the session (the header interceptor needs a token on every
 * request) while `data` writes it (login, refresh, logout). An implementation here would drag
 * Zhihu's model types across the boundary; this interface living in `data` would instead point
 * `core` at `data`.
 *
 * Reads are never cached: a write from the login screen has to be visible to the very next
 * request.
 */
interface CredentialStore {

    /**
     * The device id sent as `x-udid`.
     *
     * The server's to issue, not this app's to invent: an id made up locally is one the server
     * has never seen, and a request carrying it is answered with a 500 that names nothing —
     * including the very request that would have issued a real one.
     *
     * @return The device id, or null while none has been issued. A request made in that state
     *   goes without the header, which is what the client does on a fresh install.
     */
    fun udid(): String?

    /**
     * Replaces the device id.
     *
     * @param value Stored as-is; blank is ignored, a blank paste being a slip rather than an
     *   instruction. [clearUdid] is the deliberate way back to having none.
     */
    fun replaceUdid(value: String)

    /** Forgets the device id, so that the next bootstrap takes a fresh one. */
    fun clearUdid()

    /**
     * Reads the stored session.
     *
     * @return The session, or every field empty when nothing is stored — which the caller is
     *   expected to answer with a guest bootstrap, never by treating it as a state to stay in.
     */
    fun read(): Credentials

    /**
     * The stored session, emitting again whenever it changes.
     *
     * For what has to notice a session that changed somewhere else. Signing in and signing out
     * both write here, and a screen that draws the signed-in user has no request of its own that
     * would come back and tell it.
     */
    val session: StateFlow<Credentials>

    /**
     * Stores a session, replacing every session field.
     *
     * @param credentials The session to store. The device id is not one of them, and is left
     *   untouched.
     */
    fun save(credentials: Credentials)

    /** Drops the session, leaving the device id — that one is the device's, not the session's. */
    fun clear()
}
