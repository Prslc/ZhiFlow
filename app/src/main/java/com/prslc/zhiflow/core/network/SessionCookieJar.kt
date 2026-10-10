package com.prslc.zhiflow.core.network

import android.content.SharedPreferences
import androidx.core.content.edit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * The session's cookies, kept where OkHttp both writes and reads them.
 *
 * Nothing else may set a `Cookie` header. A jar and a hand-written header cannot both be right
 * — OkHttp replaces whatever the header said with what the jar holds — so [HttpClientProvider]
 * stopped writing one when this arrived.
 *
 * Survives restarts: the cookies outlive the process by weeks, and a sign-in whose cookies were
 * lost would come back as an unauthenticated request holding a good token.
 */
class SessionCookieJar(private val sharedPreferences: SharedPreferences) : CookieJar {

    private val lock = Any()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return

        synchronized(lock) {
            // A cookie the server sent again replaces the one held under the same name; the rest
            // are left alone, because each response carries only the cookies it touches.
            val resent = cookies.map { it.name }.toSet()
            val merged = load().filterNot { it.name in resent } + cookies
            val alive = merged.filterNot { it.isExpired(System.currentTimeMillis()) }
            sharedPreferences.edit { putString(KEY_COOKIES, alive.joinToString("\n", transform = ::encode)) }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) {
        val now = System.currentTimeMillis()
        load().filter { it.matches(url) && !it.isExpired(now) }
    }

    private fun load(): List<Cookie> =
        sharedPreferences.getString(KEY_COOKIES, null)
            ?.lineSequence()
            ?.mapNotNull(::decode)
            ?.toList()
            .orEmpty()

    private fun Cookie.isExpired(now: Long): Boolean = expiresAt <= now

    // `Cookie.toString` is not a round trip and `Cookie.parse` wants the URL the cookie came
    // from; five fields written out plainly are both, and cannot be broken by either changing.
    private fun encode(cookie: Cookie): String = listOf(
        cookie.name,
        cookie.value,
        cookie.domain,
        cookie.path,
        cookie.expiresAt.toString(),
        cookie.secure.toString(),
        cookie.httpOnly.toString(),
    ).joinToString(FIELD_SEPARATOR)

    private fun decode(line: String): Cookie? = try {
        val fields = line.split(FIELD_SEPARATOR)
        Cookie.Builder()
            .name(fields[0])
            .value(fields[1])
            .domain(fields[2])
            .path(fields[3])
            .expiresAt(fields[4].toLong())
            .apply {
                if (fields[5].toBoolean()) secure()
                if (fields[6].toBoolean()) httpOnly()
            }
            .build()
    } catch (_: Exception) {
        // A line that no longer parses is dropped rather than taken down the whole jar with it.
        null
    }

    private companion object {
        const val KEY_COOKIES = "session_cookies"

        /** Tab, because a cookie value can hold anything a header can except this. */
        const val FIELD_SEPARATOR = "\t"
    }
}
