package com.prslc.zhiflow.data.session

import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.core.network.Credentials
import com.prslc.zhiflow.data.remote.service.UserService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide cache of the logged-in user's hash id.
 *
 * The unfollow endpoint embeds this id in its URL, and nothing else in the app holds one. It is
 * resolved lazily, and what is cached is tagged with the token it was resolved under — so a
 * login, a drop to guest, or a sign-out invalidates it by changing the token, and no caller has
 * to remember to say that it should. Failures are never cached, so a later call can retry.
 */
class UserSession(
    private val userService: UserService,
    private val credentialStore: CredentialStore,
) {

    private val mutex = Mutex()

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var cachedUserId: String? = null

    /**
     * @return The current user's hash id, or a failure when unauthenticated or when the profile
     *   response carries no id.
     */
    suspend fun currentUserId(): Result<String> = mutex.withLock {
        val token = credentialStore.read().accessToken
        val cached = cachedUserId
        if (cached != null && token.isNotBlank() && token == cachedToken) {
            return@withLock Result.success(cached)
        }

        val user = userService.getUserDetail(SELF_TOKEN)
            .getOrElse { return@withLock Result.failure(it) }
        val id = user.id
        if (id.isBlank()) return@withLock Result.failure(ApiException.UnknownException())

        cachedToken = token
        cachedUserId = id
        Result.success(id)
    }

    /**
     * Seeds the cache from a session just stored, sparing the lookup [currentUserId] would make.
     *
     * A guest has nothing worth caching here: the id it carries is its own, and the URL this
     * would end up in belongs to an account.
     *
     * @param credentials The session just stored.
     */
    fun prime(credentials: Credentials) {
        if (credentials.isGuest || credentials.userId.isBlank()) return
        cachedToken = credentials.accessToken
        cachedUserId = credentials.userId
    }

    private companion object {
        /** The token that stands in for the signed-in user. */
        const val SELF_TOKEN = "self"
    }
}
