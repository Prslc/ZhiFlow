package com.prslc.zhiflow.ui.page.login

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import org.koin.androidx.compose.koinViewModel

/* Rhythm: 24dp between the brand block and the form, then 16dp between form
 * rows, so the eye reads "who this is" then "one thing to fill in" rather than
 * five equally weighted rows. */
private val ScreenPadding = 24.dp
private val BrandToForm = 32.dp
private val BetweenFields = 16.dp
private val FieldHeight = 56.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    val accountBlank = viewModel.account.isBlank()
    val secretBlank = when (viewModel.method) {
        LoginViewModel.Method.CODE -> viewModel.code.isBlank()
        LoginViewModel.Method.PASSWORD -> viewModel.password.isBlank()
    }
    var showMissing by remember { mutableStateOf(false) }

    val codeFocus = remember { FocusRequester() }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // The keyboard must not be able to cover the submit button, and
                // the form has to be reachable by scrolling on a short screen.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding)
                .padding(top = 72.dp, bottom = 40.dp),
        ) {
            BrandBlock()

            Column(
                modifier = Modifier.padding(top = BrandToForm),
                verticalArrangement = Arrangement.spacedBy(BetweenFields),
            ) {
                MethodSelector(
                    selected = viewModel.method,
                    onSelect = viewModel::selectMethod,
                )

                OutlinedTextField(
                    value = viewModel.account,
                    onValueChange = viewModel::onAccountChange,
                    label = { Text(stringResource(R.string.login_label_account)) },
                    singleLine = true,
                    isError = showMissing && accountBlank,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { codeFocus.requestFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FieldHeight),
                )

                // Swapping the two methods changes one row of the form; fading
                // rather than cutting keeps the field above and the button below
                // from appearing to jump.
                AnimatedContent(
                    targetState = viewModel.method,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "login-method",
                ) { method ->
                    when (method) {
                        LoginViewModel.Method.CODE -> Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = viewModel.code,
                                    onValueChange = viewModel::onCodeChange,
                                    label = { Text(stringResource(R.string.login_label_code)) },
                                    singleLine = true,
                                    isError = showMissing && secretBlank,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(FieldHeight)
                                        .focusRequester(codeFocus),
                                )
                                FilledTonalButton(
                                    onClick = {
                                        if (accountBlank) showMissing = true else viewModel.sendCode()
                                    },
                                    enabled = !viewModel.isSendingCode && viewModel.resendIn == 0,
                                    modifier = Modifier.height(FieldHeight),
                                ) {
                                    Text(
                                        if (viewModel.resendIn > 0) {
                                            stringResource(
                                                R.string.login_action_resend_in,
                                                viewModel.resendIn,
                                            )
                                        } else {
                                            stringResource(R.string.login_action_send_code)
                                        },
                                    )
                                }
                            }

                            // Sits under the field it describes rather than in
                            // the gap between rows, so it reads as that field's
                            // status and not as its own line of the form.
                            viewModel.codeSentTo?.let { number ->
                                Text(
                                    text = stringResource(R.string.login_code_sent, number),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            }
                        }

                        LoginViewModel.Method.PASSWORD -> PasswordField(
                            value = viewModel.password,
                            onValueChange = viewModel::onPasswordChange,
                            isError = showMissing && secretBlank,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(FieldHeight),
                        )
                    }
                }

                if (showMissing && (accountBlank || secretBlank)) {
                    Text(
                        text = stringResource(
                            when {
                                accountBlank -> {
                                    R.string.login_error_account_missing
                                }

                                viewModel.method == LoginViewModel.Method.CODE -> {
                                    R.string.login_error_code_missing
                                }

                                else -> {
                                    R.string.login_error_password_missing
                                }
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = {
                        if (accountBlank || secretBlank) {
                            showMissing = true
                        } else {
                            viewModel.submit()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(FieldHeight),
                ) {
                    // In flight it stays enabled: the view model absorbs a repeat,
                    // and a disabled button would dim the spinner along with the
                    // label.
                    if (viewModel.isSubmitting) {
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.login_action_submit),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

/* The wordmark carries the identity, so it stays in the neutral on-surface tone
 * and the accent is spent on the one action instead. Under dynamic colour a
 * brand tinted with `primary` would come out whatever the wallpaper is, which is
 * the opposite of what a wordmark is for. */
@Composable
private fun BrandBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.login_brand_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* A segmented row rather than chips: the two methods are alternatives, and only
 * one can hold, which is what a single-choice group says and what a filter chip
 * does not. */
@Composable
private fun MethodSelector(
    selected: LoginViewModel.Method,
    onSelect: (LoginViewModel.Method) -> Unit,
) {
    val methods = LoginViewModel.Method.entries

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        methods.forEachIndexed { index, method ->
            SegmentedButton(
                selected = selected == method,
                onClick = { onSelect(method) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = methods.size),
            ) {
                Text(stringResource(method.labelRes))
            }
        }
    }
}

private val LoginViewModel.Method.labelRes: Int
    get() = when (this) {
        LoginViewModel.Method.CODE -> R.string.login_method_code
        LoginViewModel.Method.PASSWORD -> R.string.login_method_password
    }

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.login_label_password)) },
        singleLine = true,
        isError = isError,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = stringResource(
                        if (visible) {
                            R.string.login_action_hide_password
                        } else {
                            R.string.login_action_show_password
                        },
                    ),
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier,
    )
}
