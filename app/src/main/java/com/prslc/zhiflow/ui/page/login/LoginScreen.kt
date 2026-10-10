package com.prslc.zhiflow.ui.page.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val actionError = viewModel.actionError
    val actionErrorText = actionError?.uiMessage

    LaunchedEffect(actionError) {
        if (actionErrorText != null) {
            viewModel.consumeActionError()
            Toast.makeText(context, actionErrorText, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel.signedIn) {
        if (viewModel.signedIn) {
            Toast.makeText(context, R.string.login_signed_in, Toast.LENGTH_SHORT).show()
            onSignedIn()
        }
    }

    val canSubmit = viewModel.account.isNotBlank() && when (viewModel.method) {
        LoginViewModel.Method.CODE -> viewModel.code.isNotBlank()
        LoginViewModel.Method.PASSWORD -> viewModel.password.isNotBlank()
    }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoginViewModel.Method.entries.forEach { method ->
                    FilterChip(
                        selected = viewModel.method == method,
                        onClick = { viewModel.selectMethod(method) },
                        label = { Text(stringResource(method.labelRes)) },
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.account,
                onValueChange = viewModel::onAccountChange,
                label = { Text(stringResource(R.string.login_label_account)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )

            when (viewModel.method) {
                LoginViewModel.Method.CODE -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = viewModel.code,
                            onValueChange = viewModel::onCodeChange,
                            label = { Text(stringResource(R.string.login_label_code)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = viewModel::sendCode,
                            enabled = !viewModel.isSendingCode &&
                                viewModel.resendIn == 0 &&
                                viewModel.account.isNotBlank(),
                        ) {
                            Text(
                                if (viewModel.resendIn > 0) {
                                    stringResource(R.string.login_action_resend_in, viewModel.resendIn)
                                } else {
                                    stringResource(R.string.login_action_send_code)
                                }
                            )
                        }
                    }

                    viewModel.codeSentTo?.let { number ->
                        Text(
                            text = stringResource(R.string.login_code_sent, number),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                LoginViewModel.Method.PASSWORD -> OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text(stringResource(R.string.login_label_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = viewModel::submit,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // In flight it stays enabled: the view model absorbs a repeat, and a disabled
                // button would dim the spinner along with the label.
                if (viewModel.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.login_action_submit))
                }
            }
        }
    }
}

private val LoginViewModel.Method.labelRes: Int
    get() = when (this) {
        LoginViewModel.Method.CODE -> R.string.login_method_code
        LoginViewModel.Method.PASSWORD -> R.string.login_method_password
    }
