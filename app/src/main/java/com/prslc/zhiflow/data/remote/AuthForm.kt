package com.prslc.zhiflow.data.remote

import com.prslc.zhiflow.core.native.Natives
import java.net.URLEncoder

/** Builds the URL-encoded forms the account endpoints take as a sealed body. */
internal object AuthForm {

    /** The app the endpoints are told the request is for. */
    const val SOURCE = "com.zhihu.android"

    const val GRANT_DIGITS = "digits"
    const val GRANT_PASSWORD = "password"
    const val GRANT_REFRESH_TOKEN = "refresh_token"

    /** A plain form, for the endpoints that carry no signature. */
    fun of(vararg fields: Pair<String, String>): String = fields.joinToString("&") { (name, value) ->
        "$name=${URLEncoder.encode(value, Charsets.UTF_8)}"
    }

    /** The sign-in form: [fields] plus the five fields every sign-in carries. */
    fun signIn(grantType: String, vararg fields: Pair<String, String>): String {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        return of(
            "grant_type" to grantType,
            "client_id" to CLIENT_ID,
            "source" to SOURCE,
            "timestamp" to timestamp,
            "signature" to signature(grantType, timestamp),
            *fields,
        )
    }

    // Signed over the raw values, never the encoded ones. The key lives in the library.
    private fun signature(grantType: String, timestamp: String): String =
        Natives.authSignature(grantType + CLIENT_ID + SOURCE + timestamp)

    // The client's own identifier: the same for every install and not a secret, so it is not
    // hidden. The secret it pairs with is the library's.
    private const val CLIENT_ID = "8d5227e0aaaa4797a763ac64e0c3b8"
}
