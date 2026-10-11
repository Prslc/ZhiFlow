package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.HeaderProvider
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.establishesSession
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.core.network.sealedBody
import com.prslc.zhiflow.data.model.account.AuthToken
import com.prslc.zhiflow.data.model.account.Captcha
import com.prslc.zhiflow.data.model.account.GuestToken
import com.prslc.zhiflow.data.remote.AuthForm
import okhttp3.Request

/** Service for obtaining and dropping a session. */
class AuthService(private val api: ApiClient) {

    /**
     * Fetches the ticket a sign-in has to carry.
     *
     * Take one per attempt: the ticket is held server-side until it is used, so one fetched for
     * an earlier attempt will not do.
     *
     * @return A [Result] carrying the ticket, and whether the server wants a human check first.
     */
    suspend fun getCaptcha(): Result<Captcha> =
        api.safeApiCall {
            Request.Builder().authRequest(PATH_CAPTCHA)
                .get()
                .build()
        }

    /**
     * Asks the server to text a login code.
     *
     * @param phoneNumber With its country code, e.g. `+8613800138000`.
     * @return A [Result] indicating whether a code was sent.
     */
    suspend fun requestDigits(phoneNumber: String): Result<Unit> =
        api.safeExecute {
            Request.Builder().authRequest(PATH_AUTH_DIGITS)
                .sealedBody(AuthForm.of("username" to phoneNumber))
                .build()
        }

    /**
     * Signs in with a code.
     *
     * @param phoneNumber The number the code was sent to, as given to [requestDigits].
     * @param code The code from the text message.
     * @return A [Result] carrying the session.
     */
    suspend fun signInWithDigits(phoneNumber: String, code: String): Result<AuthToken> =
        api.safeApiCall {
            Request.Builder().authRequest(PATH_SIGN_IN)
                .sealedBody(
                    AuthForm.signIn(
                        AuthForm.GRANT_DIGITS,
                        "username" to phoneNumber,
                        "digits" to code,
                    ),
                )
                .build()
        }

    /**
     * Signs in with a password.
     *
     * @param account A phone number or an email address.
     * @param password The account's password.
     * @return A [Result] carrying the session.
     */
    suspend fun signInWithPassword(account: String, password: String): Result<AuthToken> =
        api.safeApiCall {
            Request.Builder().authRequest(PATH_SIGN_IN)
                .sealedBody(
                    AuthForm.signIn(
                        AuthForm.GRANT_PASSWORD,
                        "username" to account,
                        "password" to password,
                    ),
                )
                .build()
        }

    /**
     * Exchanges a refresh token for a new session.
     *
     * @param refreshToken The token from the session being renewed.
     * @return A [Result] carrying the new session, which includes a new refresh token.
     */
    suspend fun refresh(refreshToken: String): Result<AuthToken> =
        api.safeApiCall {
            Request.Builder().authRequest(PATH_SIGN_IN)
                .sealedBody(
                    AuthForm.signIn(
                        AuthForm.GRANT_REFRESH_TOKEN,
                        "refresh_token" to refreshToken,
                    ),
                )
                .build()
        }

    /** Takes an anonymous session — what every request needs before anyone has logged in. */
    suspend fun guestToken(): Result<GuestToken> =
        api.safeApiCall {
            Request.Builder().authRequest(PATH_GUESTS_TOKEN)
                .sealedBody(AuthForm.of("source" to AuthForm.SOURCE))
                .establishesSession()
                .build()
        }

    /**
     * Drops the session server-side. The response's own token is not kept: the capture gives no
     * evidence it stands in for a guest session, and one is fetched anyway.
     *
     * @param accessToken The token being dropped.
     * @return A [Result] indicating whether the server accepted the drop.
     */
    suspend fun logout(accessToken: String): Result<Unit> =
        api.safeExecute {
            Request.Builder().authRequest(PATH_CLIENT_LOGOUT)
                .sealedBody(
                    AuthForm.of(
                        "token" to accessToken,
                        "use_refresh_direct_sign" to "0",
                    ),
                )
                .build()
        }

    // The capture shows x-api-version on these requests; no other service here sets it, so it is
    // pinned here rather than in the interceptor that everything shares.
    private fun Request.Builder.authRequest(path: String): Request.Builder =
        apiUrl(path).header("x-api-version", HeaderProvider.API_VERSION)

    private companion object {
        const val PATH_CAPTCHA = "/captcha"
        const val PATH_AUTH_DIGITS = "/api/account/prod/auth/digits"
        const val PATH_SIGN_IN = "/api/account/prod/sign_in"
        const val PATH_GUESTS_TOKEN = "/api/account/prod/guests/token"
        const val PATH_CLIENT_LOGOUT = "/api/account/prod/client_logout"
    }
}
