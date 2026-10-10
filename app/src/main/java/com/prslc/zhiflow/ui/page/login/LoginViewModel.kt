package com.prslc.zhiflow.ui.page.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    enum class Method { CODE, PASSWORD }

    var method by mutableStateOf(Method.CODE)
        private set
    var account by mutableStateOf("")
        private set
    var code by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isSendingCode by mutableStateOf(false)
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<ApiException?>(null)
        private set

    /** Set once a session has been stored, which is the screen's cue to leave. */
    var signedIn by mutableStateOf(false)
        private set

    /** The number a code was last sent to, or null while none has been. */
    var codeSentTo by mutableStateOf<String?>(null)
        private set

    /** Seconds left before another code may be asked for; 0 when one may. */
    var resendIn by mutableIntStateOf(0)
        private set

    private var cooldownJob: Job? = null

    fun selectMethod(value: Method) {
        method = value
    }

    fun onAccountChange(value: String) {
        account = value
        // The hint names a number, so it stops applying the moment that number is edited away.
        codeSentTo = null
    }

    fun onCodeChange(value: String) {
        code = value
    }

    fun onPasswordChange(value: String) {
        password = value
    }

    /** Asks for a code. No-op while one is on its way, or one has just been sent. */
    fun sendCode() {
        val number = account.trim()
        if (number.isBlank() || isSendingCode || resendIn > 0) return

        isSendingCode = true
        viewModelScope.launch {
            authRepository.requestDigits(number)
                .onSuccess {
                    // Kept on screen: the reader needs to know which number to wait on.
                    codeSentTo = number
                    startCooldown()
                }
                .onApiFailure { actionError = it }
            isSendingCode = false
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in RESEND_COOLDOWN_SECONDS downTo 1) {
                resendIn = remaining
                delay(1_000)
            }
            resendIn = 0
        }
    }

    /** Signs in by whichever [method] is selected. No-op while one is already in flight. */
    fun submit() {
        val number = account.trim()
        if (number.isBlank() || isSubmitting) return

        isSubmitting = true
        viewModelScope.launch {
            val outcome = when (method) {
                Method.CODE -> authRepository.signInWithDigits(number, code.trim())
                Method.PASSWORD -> authRepository.signInWithPassword(number, password)
            }
            outcome.onSuccess { signedIn = true }
            outcome.onApiFailure { actionError = it }
            isSubmitting = false
        }
    }

    fun consumeActionError() {
        actionError = null
    }

    private companion object {
        /** How long the resend button stays held after one code has gone out. */
        const val RESEND_COOLDOWN_SECONDS = 60
    }
}
