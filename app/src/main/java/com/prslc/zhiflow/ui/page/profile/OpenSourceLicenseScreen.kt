package com.prslc.zhiflow.ui.page.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryDetailMode
import com.prslc.zhiflow.R

@Composable
fun OpenSourceLicenseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Generated at build time by the aboutlibraries plugin, which walks the resolved
    // dependencies — so the list follows the build rather than being kept by hand.
    val libraries by produceLibraries(R.raw.aboutlibraries)
    var selected by remember { mutableStateOf<Library?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
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
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp),
            // Rows carry name and license only; the text a reader wants next is one tap away in
            // the dialog below, which is the one place it can be shown whole.
            detailMode = LibraryDetailMode.None,
            onLibraryClick = { library ->
                selected = library
                true
            },
        )
    }

    selected?.let { library ->
        val uriHandler = LocalUriHandler.current
        val website = library.website

        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(library.name) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    library.licenses.forEach { license ->
                        Text(
                            text = license.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = license.licenseContent
                                ?: stringResource(R.string.licenses_no_text),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) {
                    Text(stringResource(R.string.general_close))
                }
            },
            dismissButton = if (website != null) {
                {
                    TextButton(onClick = { uriHandler.openUri(website) }) {
                        Text(stringResource(R.string.licenses_visit))
                    }
                }
            } else {
                null
            },
        )
    }
}
