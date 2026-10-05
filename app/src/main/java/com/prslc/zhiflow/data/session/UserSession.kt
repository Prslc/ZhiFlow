package com.prslc.zhiflow.data.session

import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.data.remote.service.UserService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide, in-memory cache of the logged-in user's hash id.
 *
 * The unfollow endpoint embeds this id in its URL, but nothing in the app
 * persists one: `GET /people/self` is the only source. It is resolved lazily
 * on first use and kept for the process lifetime; failures are never cached,
 * so a later call can retry once credentials are fixed.
 *
 * Call [invalidate] whenever credentials change.
 */
class UserSession(private val userService: UserService) {

    private val mutex = Mutex()

    @Volatile
    private var cachedUserId: String? = null

    /**
     * @return The current user's hash id, or a failure when unauthenticated or
     *         when the profile response carries no id.
     */
    suspend fun currentUserId(): Result<String> = mutex.withLock {
        cachedUserId?.let { return@withLock Result.success(it) }

        val user = userService.getUserDetail("self")
            .getOrElse { return@withLock Result.failure(it) }
        val id = user.id
        if (id.isBlank()) return@withLock Result.failure(ApiException.UnknownException())

        cachedUserId = id
        Result.success(id)
    }

    /** Seeds the cache from an already-fetched profile, sparing a second lookup. */
    fun prime(userId: String) {
        if (userId.isNotBlank()) cachedUserId = userId
    }

    /** Drops the cached id. Must be called whenever credentials change. */
    fun invalidate() {
        cachedUserId = null
    }
}
