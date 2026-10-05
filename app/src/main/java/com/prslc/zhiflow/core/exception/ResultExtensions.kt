package com.prslc.zhiflow.core.exception

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
