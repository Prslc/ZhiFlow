package com.prslc.zhiflow.data.exception

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.io.IOException

fun Throwable.toApiException(): ApiException {
    return when (this) {
        is IOException -> ApiException.NetworkException()
        is ClientRequestException -> {
            when (response.status.value) {
                401, 403 -> ApiException.UnAuthorizedException()
                404 -> ApiException.NotFoundException()
                else -> ApiException.ServerException(response.status.value)
            }
        }
        is ServerResponseException -> ApiException.ServerException(response.status.value)
        is ApiException -> this
        else -> ApiException.UnknownException()
    }
}