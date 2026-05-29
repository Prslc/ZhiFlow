package com.prslc.zhiflow.core.exception

import androidx.compose.runtime.Composable
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
sealed class ApiException(val resId: Int, val code: Int? = null) : Exception() {
    class NetworkException : ApiException(R.string.error_network)
    class UnAuthorizedException : ApiException(R.string.error_unauthorized)
    class NotFoundException : ApiException(R.string.error_not_found)
    class ServerException(code: Int) : ApiException(R.string.error_server, code)
    class UnknownException : ApiException(R.string.error_unknown)

    fun getMessage(resources: android.content.res.Resources): String {
        return if (code != null) {
            resources.getString(resId, code)
        } else {
            resources.getString(resId)
        }
    }
}

val ApiException.uiMessage: String
    @Composable
    get() = getMessage(LocalResources.current)
