package com.prslc.zhiflow.core.network

import androidx.compose.runtime.Immutable

/**
 * The session a request is authenticated with: everything that changes when the user logs in,
 * logs out, or the token is refreshed.
 *
 * Holds no cookies — those belong to the [okhttp3.CookieJar], the one place that sees the
 * `Set-Cookie` meant for the next request — and no `x-udid`, which outlives any session and so
 * lives on [CredentialStore] instead.
 *
 * @property accessToken sent as `Authorization`.
 * @property refreshToken what a refresh exchanges for a new session. A guest session has none,
 *   which is why [isGuest] reads [userType] rather than inferring it from this being blank.
 * @property userId the hash id `people/self` would return.
 * @property userType the server's own word for what kind of session this is.
 * @property nextRefreshAt epoch millis at which the session should be refreshed; written as 70%
 *   of the token's lifetime. [NEVER] means "do not schedule", which is how a guest is held.
 */
@Immutable
data class Credentials(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val userType: String = "",
    val nextRefreshAt: Long = NEVER,
) {
    /** True for an anonymous session, which is never refreshed on a schedule. */
    val isGuest: Boolean get() = userType == USER_TYPE_GUEST

    val hasToken: Boolean get() = accessToken.isNotBlank()

    companion object {
        const val USER_TYPE_GUEST = "guest"

        /** A [nextRefreshAt] that never comes due. */
        const val NEVER = Long.MAX_VALUE
    }
}
