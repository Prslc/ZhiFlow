package com.prslc.zhiflow.core.network

import java.net.URLEncoder

/**
 * The URL-encoded plaintext that a sealed body carries.
 *
 * A field the caller has nothing for is left out rather than sent empty, which is what the client
 * does with the ones it has no value for; an empty `source_type=` is not the same request as none.
 *
 * @param fields The fields, in the order the endpoint is known to take them.
 * @return The form string, ready to be sealed.
 */
internal fun formOf(vararg fields: Pair<String, String?>): String =
    fields.mapNotNull { (name, value) ->
        value?.takeIf { it.isNotEmpty() }
            ?.let { "$name=${URLEncoder.encode(it, Charsets.UTF_8)}" }
    }.joinToString("&")
