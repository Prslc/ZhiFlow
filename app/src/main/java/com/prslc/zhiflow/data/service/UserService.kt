package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.data.model.ZhihuUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess

class UserService(private val client: HttpClient) {
    suspend fun getUserDetail(): ZhihuUser? {
        val response = client.get("people/self")
        return if (response.status.isSuccess()) response.body() else null
    }
}