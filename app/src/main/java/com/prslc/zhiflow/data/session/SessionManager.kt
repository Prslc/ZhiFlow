package com.prslc.zhiflow.data.session

import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.core.network.SessionGate
import com.prslc.zhiflow.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps a session in hand for every request, and renews it before it expires.
 *
 * An anonymous session is fetched when a request finds none stored — behind the device id it
 * needs, which the server issues once per install — and a session is refreshed by the response
 * that finds it past the point marked on it.
 */
class SessionManager(
    private val credentialStore: CredentialStore,
    private val authRepository: AuthRepository,
) : SessionGate {

    // Its own scope, not a caller's: a refresh has to outlive the request that noticed it was
    // due, and must not be canceled along with whichever screen made that request.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bootstrapMutex = Mutex()
    private val refreshing = AtomicBoolean(false)

    private val _droppedToGuest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Emits when a refused session was replaced by an anonymous one.
     *
     * The reader is owed this: they were signed in a moment ago, and the next thing they tap may
     * quietly behave differently. Nothing here says it — a toast or a screen does, because only
     * a surface knows how to say anything.
     */
    val droppedToGuest: SharedFlow<Unit> = _droppedToGuest.asSharedFlow()

    @Volatile
    private var nextBootstrapAttemptAt = 0L

    /**
     * Fetches an anonymous session now, if none is stored.
     *
     * Called at startup so the fetch runs underneath composing the first screen; the funnel
     * waits on the same work regardless.
     */
    fun prefetchGuestSession() {
        if (credentialStore.read().hasToken) return
        scope.launch { awaitReady() }
    }

    override suspend fun awaitReady() {
        if (credentialStore.read().hasToken) return

        bootstrapMutex.withLock {
            if (credentialStore.read().hasToken) return@withLock
            if (System.currentTimeMillis() < nextBootstrapAttemptAt) return@withLock
            if (!ensureDeviceId()) return@withLock

            authRepository.startGuestSession().onApiFailure {
                // Nothing to tell anyone: the request goes out without credentials and its own
                // failure is what the reader sees. The delay is what keeps every request in
                // flight from retrying the bootstrap at once while the network is down.
                nextBootstrapAttemptAt = System.currentTimeMillis() + BOOTSTRAP_RETRY_DELAY_MS
            }
        }
    }

    /**
     * Takes a device id if this install has none.
     *
     * A guest session cannot be had without one, and the id is the server's to give, so this is
     * a request of its own that has to land first. One round trip, once per install.
     *
     * @return Whether there is an id to go on with. A failure backs off like the session's, so
     *   a server that is down is not asked again by every request in flight.
     */
    private suspend fun ensureDeviceId(): Boolean {
        if (credentialStore.udid() != null) return true
        return authRepository.bootstrapDevice().onApiFailure {
            nextBootstrapAttemptAt = System.currentTimeMillis() + BOOTSTRAP_RETRY_DELAY_MS
        }.isSuccess
    }

    override fun onExchange(status: Int) {
        val credentials = credentialStore.read()
        if (credentials.isGuest || !credentials.hasToken) return

        val due = System.currentTimeMillis() >= credentials.nextRefreshAt
        if (due || status == HTTP_UNAUTHORIZED) refreshInBackground()
    }

    private fun refreshInBackground() {
        if (!refreshing.compareAndSet(false, true)) return

        scope.launch {
            try {
                authRepository.refresh().onApiFailure { error ->
                    // A refusal is not the same failure as an unreachable server: one means these
                    // credentials are finished with, the other means try again later.
                    if (error is ApiException.UnAuthorizedException) dropToGuest() else backOff()
                }
            } finally {
                refreshing.set(false)
            }
        }
    }

    /**
     * Pushes the next attempt out after a failure that says nothing about the credentials.
     *
     * Without it the due time stays in the past, and every response still in flight starts
     * another attempt at a server that has just refused one.
     */
    private fun backOff() {
        val session = credentialStore.read()
        credentialStore.save(
            session.copy(nextRefreshAt = System.currentTimeMillis() + REFRESH_RETRY_DELAY_MS),
        )
    }

    private suspend fun dropToGuest() {
        val outcome = authRepository.startGuestSession()
        outcome.onSuccess { _droppedToGuest.tryEmit(Unit) }
        outcome.onApiFailure {
            nextBootstrapAttemptAt = System.currentTimeMillis() + BOOTSTRAP_RETRY_DELAY_MS
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val BOOTSTRAP_RETRY_DELAY_MS = 30_000L
        const val REFRESH_RETRY_DELAY_MS = 24 * 60 * 60 * 1000L
    }
}
