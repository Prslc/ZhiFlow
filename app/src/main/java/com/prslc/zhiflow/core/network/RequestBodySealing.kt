package com.prslc.zhiflow.core.network

import com.prslc.zhiflow.core.native.Natives
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Marks a request whose body was sealed. */
private object SealedBody

/**
 * Seals [form] into this request's body, and marks it as one that carries no signature.
 *
 * The mark travels with the seal on purpose: a sealed request must not be signed, and keeping
 * the two in one call is what stops a new sealed endpoint from being signed by accident — which
 * a path list in the interceptor would allow.
 */
internal fun Request.Builder.sealedBody(form: String): Request.Builder =
    post(Natives.sealBody(form).toByteArray(Charsets.UTF_8).toRequestBody(FORM_MEDIA_TYPE))
        .tag(SealedBody::class.java, SealedBody)

/** True when this request's body was sealed, and so must go unsigned. */
internal fun Request.hasSealedBody(): Boolean = tag(SealedBody::class.java) != null

// No charset: the client sends this body as a bare `application/x-www-form-urlencoded`, and a
// `; charset=utf-8` appended to it is one more thing for the gateway to have an opinion about.
private val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded".toMediaType()
