package com.prslc.zhiflow.core.exception

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import com.prslc.zhiflow.R

/**
 * Domain-specific exception hierarchy for API errors.
 *
 * - [NetworkException]: connectivity errors (timeout, DNS, etc.)
 * - [UnAuthorizedException]: HTTP 401/403
 * - [NotFoundException]: HTTP 404
 * - [ServerException]: other 4xx/5xx with HTTP status code
 * - [UnknownException]: unmatched errors
 */
sealed class ApiException(
    val resId: Int,
    val code: Int? = null,
    /**
     * The server's own wording for this failure, when it sent one. Preferred over [resId]:
     * it names what actually went wrong, where [resId] can only name the status code.
     */
    val serverMessage: String? = null,
) : Exception() {
    class NetworkException : ApiException(R.string.error_network)

    class UnAuthorizedException : ApiException(R.string.error_unauthorized)

    class NotFoundException : ApiException(R.string.error_not_found)

    class ServerException(code: Int, serverMessage: String? = null) :
        ApiException(R.string.error_server, code, serverMessage)

    class UnknownException : ApiException(R.string.error_unknown)

    fun getMessage(resources: android.content.res.Resources): String {
        serverMessage?.takeIf { it.isNotBlank() }?.let { return it }
        return if (code != null) {
            resources.getString(resId, code)
        } else {
            resources.getString(resId)
        }
    }
}

val ApiException.uiMessage: String
    @Composable
    @ReadOnlyComposable
    get() = getMessage(LocalResources.current)
