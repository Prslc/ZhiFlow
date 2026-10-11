package com.prslc.zhiflow.core.exception

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs [block] where a page of network data can be turned into what a screen draws.
 *
 * Which thread that is has nothing to do with who asked: a repository is called from wherever the
 * screen that wants it happens to be, and through a ViewModel's `viewModelScope` that is the main
 * thread. Mapping a page of models is not the main thread's work.
 */
internal suspend fun <R> offMain(block: () -> R): R = withContext(Dispatchers.Default) { block() }

/**
 * [offMain] for a [Result] whose value is a whole page: one hop per page rather than one per row,
 * with the transform itself unchanged.
 */
internal suspend fun <T, R> Result<T>.mapOffMain(transform: (T) -> R): Result<R> =
    offMain { map(transform) }

/**
 * Like [Result.onFailure], but the callback always receives a non-null [ApiException]:
 * an unrecognised throwable is mapped to [ApiException.UnknownException] rather than
 * dropped, so a failure can never turn into a silent no-op. [toApiException] re-throws
 * [kotlin.coroutines.cancellation.CancellationException] before the callback runs.
 *
 * This is the only way the ViewModel layer handles a failed [Result].
 */
internal inline fun <T> Result<T>.onApiFailure(action: (ApiException) -> Unit): Result<T> =
    onFailure { action(it.toApiException()) }

/**
 * Deliberately drops a [Result] whose failure the caller does not act on — best-effort work
 * such as reading-progress sync. Exists so that "we do not care" is a visible decision
 * (`grep ignoreOutcome`) rather than looking like a forgotten error check.
 */
internal fun Result<*>.ignoreOutcome() = Unit
