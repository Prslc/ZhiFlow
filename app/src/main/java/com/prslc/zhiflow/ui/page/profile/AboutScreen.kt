package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prslc.zhiflow.BuildConfig
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.preference.SettingsStore
import com.prslc.zhiflow.ui.component.preference.SettingsGroup
import com.prslc.zhiflow.ui.component.preference.SettingsNavRow
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Where the app's source lives — what a tap on the row below opens. */
private const val SOURCE_URL = "https://github.com/Prslc/ZhiFlow"

/** Taps on the version number that unlock the developer tools, the count the platform asks for. */
private const val DEVELOPER_TAPS = 7

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val settingsStore = koinInject<SettingsStore>()
    val developerUnlocked by settingsStore.developerUnlocked.collectAsStateWithLifecycle()
    var taps by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Every line the tap below can say, read here because its handler is not a composable and
    // could not read a string resource itself. The countdown is the one that depends on taps.
    val hintOnNextTap = pluralStringResource(
        R.plurals.about_developer_hint,
        DEVELOPER_TAPS - taps - 1,
        DEVELOPER_TAPS - taps - 1,
    )
    val unlockedText = stringResource(R.string.about_developer_unlocked)
    val alreadyText = stringResource(R.string.about_developer_already)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The launcher's foreground layer rather than the adaptive icon: `painterResource`
            // reads a vector, and an `<adaptive-icon>` is not one.
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable {
                    val message = if (developerUnlocked) {
                        alreadyText
                    } else {
                        taps++
                        if (taps >= DEVELOPER_TAPS) {
                            settingsStore.setDeveloperUnlocked(true)
                            unlockedText
                        } else {
                            hintOnNextTap
                        }
                    }
                    scope.launch {
                        // A run of taps must not queue up behind one another: each one replaces
                        // whatever is on screen.
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(message)
                    }
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup {
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.about_view_source_title),
                        description = stringResource(R.string.about_view_source_summary),
                        icon = Icons.Filled.Code,
                        onClick = { uriHandler.openUri(SOURCE_URL) },
                    )
                }
                row {
                    SettingsNavRow(
                        title = stringResource(R.string.about_licenses_title),
                        description = stringResource(R.string.about_licenses_summary),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = navigator::navigateToOpenSourceLicense,
                    )
                }
            }
        }
    }
}
