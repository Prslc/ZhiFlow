package com.prslc.zhiflow.ui.page.profile

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.preference.AppLanguage
import com.prslc.zhiflow.core.preference.ThemeMode
import com.prslc.zhiflow.ui.component.preference.SettingsGroup
import com.prslc.zhiflow.ui.component.preference.SettingsNavRow
import com.prslc.zhiflow.ui.component.preference.SettingsRow
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language = viewModel.language
    val dataSaver by viewModel.dataSaver.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val developerUnlocked by viewModel.developerUnlocked.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadCacheSize() }

    // A sign-out is answered by leaving: the profile behind this page is the one that then shows
    // the reader browsing as a guest.
    LaunchedEffect(viewModel.signedOut) {
        if (viewModel.signedOut) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.general_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsGroup(title = stringResource(R.string.settings_section_appearance)) {
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.settings_theme_title),
                        description = themeMode.label(),
                        icon = Icons.Filled.Contrast,
                        onClick = { showThemeDialog = true },
                    )
                }
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.settings_language_title),
                        description = language.label(),
                        icon = Icons.Filled.Translate,
                        onClick = { showLanguageDialog = true },
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.settings_section_general)) {
                row {
                    SettingsRow(
                        title = stringResource(R.string.settings_data_saver_title),
                        description = stringResource(R.string.settings_data_saver_summary),
                        icon = Icons.Filled.DataSaverOn,
                        onClick = { viewModel.setDataSaver(!dataSaver) },
                        trailingContent = {
                            Switch(checked = dataSaver, onCheckedChange = null)
                        },
                    )
                }
                row {
                    SettingsRow(
                        title = stringResource(R.string.settings_search_history_title),
                        description = stringResource(R.string.settings_search_history_summary),
                        icon = Icons.Filled.History,
                        onClick = { viewModel.setSearchHistory(!searchHistory) },
                        trailingContent = {
                            Switch(checked = searchHistory, onCheckedChange = null)
                        },
                    )
                }
                row {
                    SettingsRow(
                        title = stringResource(R.string.settings_image_cache_title),
                        description = Formatter.formatFileSize(context, viewModel.cacheSize),
                        icon = Icons.Filled.Image,
                        enabled = !viewModel.isClearing,
                        onClick = { showClearDialog = true },
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.settings_section_about)) {
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.settings_about_title),
                        description = stringResource(R.string.settings_about_summary),
                        icon = Icons.Filled.Info,
                        onClick = navigator::navigateToAbout,
                    )
                }
            }

            if (developerUnlocked) {
                SettingsGroup(title = stringResource(R.string.settings_section_developer)) {
                    row {
                        SettingsNavRow(
                            title = stringResource(R.string.debug_title),
                            description = stringResource(R.string.settings_developer_summary),
                            icon = Icons.Filled.BugReport,
                            onClick = navigator::navigateToDebugTools,
                        )
                    }
                }
            }
            // A guest has no session to end: signing out would leave them where they already are.
            if (session.hasToken && !session.isGuest) {
                SettingsGroup(title = stringResource(R.string.settings_section_account)) {
                    row {
                        SettingsNavRow(
                            title = stringResource(R.string.settings_sign_out),
                            description = stringResource(R.string.settings_sign_out_summary),
                            icon = Icons.AutoMirrored.Filled.Logout,
                            onClick = { showSignOutDialog = true },
                        )
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_theme_title),
            options = ThemeMode.entries,
            selected = themeMode,
            label = { it.label() },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_language_title),
            options = AppLanguage.entries,
            selected = language,
            label = { it.label() },
            onSelect = {
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_image_cache_confirm_title)) },
            text = { Text(stringResource(R.string.settings_image_cache_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearCache()
                    },
                ) {
                    Text(stringResource(R.string.settings_image_cache_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.general_cancel))
                }
            },
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.settings_sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.settings_sign_out_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    },
                ) {
                    Text(stringResource(R.string.settings_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.general_cancel))
                }
            },
        )
    }
}

/**
 * A dialog of choices, one of which is already taken.
 *
 * @param title What the choices are for.
 * @param options The choices, in the order they are drawn.
 * @param selected The one in force.
 * @param label How one choice is written in the reader's language.
 * @param onSelect Called with the choice that was tapped.
 * @param onDismiss Called when the dialog is dismissed without a choice.
 */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.general_close))
            }
        },
    )
}

/** @return How the reader's language writes this theme. */
@Composable
@ReadOnlyComposable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.System -> R.string.settings_theme_system
        ThemeMode.Light -> R.string.settings_theme_light
        ThemeMode.Dark -> R.string.settings_theme_dark
    },
)

/** @return The language's own name, which is how it is written in every language. */
@Composable
@ReadOnlyComposable
private fun AppLanguage.label(): String = stringResource(
    when (this) {
        AppLanguage.System -> R.string.settings_language_system
        AppLanguage.Chinese -> R.string.settings_language_chinese
        AppLanguage.English -> R.string.settings_language_english
    },
)
