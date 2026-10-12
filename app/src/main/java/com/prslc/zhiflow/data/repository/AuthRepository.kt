package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.mapOffMain
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.core.network.Credentials
import com.prslc.zhiflow.data.mapper.toCredentials
import com.prslc.zhiflow.data.remote.service.AuthService
import com.prslc.zhiflow.data.session.UserSession

/** Owns the session: obtains it, renews it, drops it, and keeps [CredentialStore] in step. */
class AuthRepository(
    private val authService: AuthService,
    private val credentialStore: CredentialStore,
    private val userSession: UserSession,
) {

    /**
     * Takes the device id this install is known by, and keeps it.
     *
     * Runs ahead of the first anonymous session, which cannot be had without one: the id is the
     * server's to issue, and it answers a device it has never seen with a 500 rather than with
     * a session.
     *
     * @return A [Result] carrying the issued device id.
     */
    suspend fun bootstrapDevice(): Result<String> =
        authService.bootstrapDevice().mapOffMain { device ->
            credentialStore.replaceUdid(device.udid)
            device.udid
        }

    /**
     * Takes an anonymous session — what lets the app be used before anyone has logged in.
     *
     * @return A [Result] carrying the stored session.
     */
    suspend fun startGuestSession(): Result<Credentials> =
        authService.guestToken().mapOffMain { store(it.toCredentials()) }

    /**
     * Asks for a login code.
     *
     * Fetches a ticket first — the request is refused without one — and that same ticket has to
     * serve the sign-in that follows, so [signInWithDigits] must not fetch another.
     *
     * @param phoneNumber With its country code, e.g. `+8613800138000`.
     * @return A [Result] indicating whether a code was sent.
     */
    suspend fun requestDigits(phoneNumber: String): Result<Unit> {
        authService.getCaptcha().getOrElse { return Result.failure(it) }
        return authService.requestDigits(phoneNumber)
    }

    /**
     * Signs in with a texted code.
     *
     * @param phoneNumber The number the code was sent to.
     * @param code The code from the message.
     * @return A [Result] carrying the stored session.
     */
    suspend fun signInWithDigits(phoneNumber: String, code: String): Result<Credentials> =
        authService.signInWithDigits(phoneNumber, code).mapOffMain { store(it.toCredentials(System.currentTimeMillis())) }

    /**
     * Signs in with a password.
     *
     * Begins its own attempt, so it fetches its own ticket: a password sign-in is one step,
     * unlike the two-step code flow that shares one.
     *
     * @param account A phone number or an email address.
     * @param password The account's password.
     * @return A [Result] carrying the stored session.
     */
    suspend fun signInWithPassword(account: String, password: String): Result<Credentials> {
        authService.getCaptcha().getOrElse { return Result.failure(it) }
        return authService.signInWithPassword(account, password).mapOffMain { store(it.toCredentials(System.currentTimeMillis())) }
    }

    /**
     * Renews the stored session.
     *
     * @return A [Result] carrying the new session, or a failure when there is no refresh token
     *   to renew with — true of a guest session, where a refresh is never attempted.
     */
    suspend fun refresh(): Result<Credentials> {
        val refreshToken = credentialStore.read().refreshToken
        if (refreshToken.isBlank()) {
            return Result.failure(ApiException.UnAuthorizedException())
        }
        return authService.refresh(refreshToken).mapOffMain { store(it.toCredentials(System.currentTimeMillis())) }
    }

    /**
     * Drops the session, here and on the server.
     *
     * Storage is cleared even when the server refuses: the caller asked to be logged out, and
     * failing to tell the server so is not a reason to stay logged in on this device.
     *
     * @return A [Result] carrying the server's answer, if one was asked for.
     */
    suspend fun logout(): Result<Unit> {
        val accessToken = credentialStore.read().accessToken
        val result = if (accessToken.isBlank()) {
            Result.success(Unit)
        } else {
            authService.logout(accessToken)
        }
        credentialStore.clear()
        return result
    }

    private fun store(credentials: Credentials): Credentials {
        credentialStore.save(credentials)
        userSession.prime(credentials)
        return credentials
    }
}
