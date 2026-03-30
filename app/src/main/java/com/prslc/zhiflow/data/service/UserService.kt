package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.ZhihuUser
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Fetch current user profile
 */
suspend fun getUserDetail(): ZhihuUser? {
    val tag = "userService"
    return try {
        val response = Client.client.get("people/self")
        if (response.status.value != 200) {
            Log.e(tag, "Request failed with status: ${response.status}")
            return null
        }

        response.body<ZhihuUser>()
    } catch (e: Exception) {
        Log.e(tag, "Failed to fetch profile", e)
        throw e
    }
}