package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.core.network.Credentials
import com.prslc.zhiflow.data.model.account.AuthToken
import com.prslc.zhiflow.data.model.account.GuestToken

/**
 * The point at which a session comes due for a refresh, as a percentage of the token's
 * lifetime. Early enough that the request which finds it due still has a valid token to send.
 */
private const val REFRESH_AT_PERCENT = 70L

/**
 * Maps a signed-in session onto the stored one.
 *
 * `user_type` is deliberately left blank: the server's sign-in response does not carry one, and
 * only [Credentials.USER_TYPE_GUEST] means guest. Blank is therefore "a real account".
 *
 * @param now The clock reading to schedule the refresh against.
 * @return The session to store.
 */
internal fun AuthToken.toCredentials(now: Long): Credentials = Credentials(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = uid,
    nextRefreshAt = now + expiresIn * 1000 * REFRESH_AT_PERCENT / 100,
)

/**
 * Maps an anonymous session onto the stored one.
 *
 * The `id` the server returns here is the guest's own hash id, and is dropped rather than kept
 * in [Credentials.userId]: that field feeds the unfollow URL, where a guest id would address
 * the wrong person. Its absence also means a `prime` call with it does nothing.
 *
 * @return The session to store, which is never scheduled for a refresh.
 */
internal fun GuestToken.toCredentials(): Credentials = Credentials(
    accessToken = accessToken,
    userType = userType,
    nextRefreshAt = Credentials.NEVER,
)
