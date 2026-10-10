package com.prslc.zhiflow.core.network

import android.util.Base64
import java.security.SecureRandom

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
     * Generated and stored on first use, so the id is stable across restarts.
     *
     * @return The device id; never blank.
     */
    fun udid(): String

    /**
     * Replaces the device id.
     *
     * @param value Stored as-is; blank is ignored.
     */
    fun replaceUdid(value: String)

    /**
     * Reads the stored session.
     *
     * @return The session, or every field empty when nothing is stored — which the caller is
     *   expected to answer with a guest bootstrap, never by treating it as a state to stay in.
     */
    fun read(): Credentials

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

/**
 * Generates a device id of the shape the client sends: base64 of 32 random bytes.
 *
 * Random, not read off the device: this client only needs an id that is stable for this
 * install, and has no business claiming to be a particular piece of hardware.
 */
internal fun newDeviceId(): String =
    Base64.encodeToString(ByteArray(32).also(SecureRandom()::nextBytes), Base64.NO_WRAP)
