package com.prslc.zhiflow.ui.component.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.uiMessage
import kotlinx.coroutines.launch

/**
 * Turns a failed user action into a one-shot snackbar and reports it consumed.
 *
 * Hand the returned state to `Scaffold(snackbarHost = { SnackbarHost(state) })`.
 *
 * Consumption happens before the snackbar is shown, on a scope that outlives this effect:
 * [SnackbarHostState.showSnackbar] suspends for as long as the snackbar is visible, so
 * consuming afterwards would skip the callback whenever the screen is torn down mid-display,
 * leaving the stale error to pop again on the next visit.
 *
 * @param error The screen's pending failure, or null when there is nothing to show.
 * @param onConsumed Called once the error has been taken, before the snackbar appears.
 */
@Composable
fun rememberActionErrorHost(
    error: ApiException?,
    onConsumed: () -> Unit,
): SnackbarHostState {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val message = error?.uiMessage

    LaunchedEffect(error) {
        if (message == null) return@LaunchedEffect
        onConsumed()
        scope.launch { hostState.showSnackbar(message) }
    }

    return hostState
}
