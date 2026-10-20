package com.prslc.zhiflow.ui.page.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.component.common.EmptyView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Where the licensee plugin leaves its report: `androidAssetReportPath` in `app/build.gradle.kts`.
 *
 * The build resolves the dependency graph and writes what it found, so this page lists the
 * libraries the APK actually carries rather than a list kept by hand.
 */
private const val REPORT_ASSET = "licenses.json"

/**
 * The report's shape, as the plugin writes it. Every field is given a default for the same reason
 * the API models are: a report from another version should leave a field out without taking the
 * page down.
 */
@Immutable
@Serializable
private data class LicenseArtifact(
    val groupId: String = "",
    val artifactId: String = "",
    val version: String = "",
    val name: String? = null,
    val spdxLicenses: List<SpdxLicense> = emptyList(),
    val unknownLicenses: List<UnknownLicense> = emptyList(),
    val scm: ArtifactScm? = null,
) {
    /** The library's own name, or the artifact id when its metadata gave none. */
    val displayName: String get() = name ?: artifactId

    /** The group and artifact, which is what tells two libraries of the same name apart. */
    val module: String get() = "$groupId:$artifactId"

    /** How the report identifies the artifact, which is also what tells two rows apart. */
    val coordinate: String get() = "$groupId:$artifactId:$version"
}

@Immutable
@Serializable
private data class SpdxLicense(
    val identifier: String = "",
    val name: String = "",
    val url: String = "",
) {
    /** The name the license goes by, or its identifier when the report gave no name. */
    val label: String get() = name.ifEmpty { identifier }
}

@Immutable
@Serializable
private data class UnknownLicense(
    val name: String? = null,
    val url: String? = null,
)

@Immutable
@Serializable
private data class ArtifactScm(val url: String = "")

/** The names of every license the artifact declared, in the report's own wording. */
private val LicenseArtifact.licenseNames: List<String>
    get() = spdxLicenses.map { it.label } + unknownLicenses.mapNotNull { it.name ?: it.url }

private val ReportJson = Json { ignoreUnknownKeys = true }

/** A surface the page draws itself, rounded the way the rest of the app rounds one. */
private val SurfaceShape = RoundedCornerShape(16.dp)

/**
 * Where a tap on the card goes: the license's canonical text, which is what the page is about.
 * The project page answers when the report gave no license URL.
 */
private val LicenseArtifact.licenseUrl: String?
    get() = spdxLicenses.firstOrNull { it.url.isNotBlank() }?.url
        ?: unknownLicenses.firstOrNull { !it.url.isNullOrBlank() }?.url
        ?: scm?.url?.takeIf { it.isNotBlank() }

@Composable
fun OpenSourceLicenseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val report by produceState<List<LicenseArtifact>?>(null, context) {
        value = try {
            readReport(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

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
        report?.let { artifacts ->
            if (artifacts.isEmpty()) {
                EmptyView(
                    message = stringResource(R.string.licenses_unavailable),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp,
                    ),
                ) {
                    items(artifacts, key = { it.coordinate }) { artifact ->
                        LicenseCard(
                            artifact = artifact,
                            onClick = { artifact.licenseUrl?.let(uriHandler::openUri) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reads the report and sorts it into the order it is drawn: the list is a name index, so a reader
 * looking for a library they know finds it where its name would be on a shelf.
 */
private suspend fun readReport(context: Context): List<LicenseArtifact> =
    withContext(Dispatchers.IO) {
        val text = context.assets.open(REPORT_ASSET).bufferedReader().use { it.readText() }
        ReportJson.decodeFromString<List<LicenseArtifact>>(text)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
    }

/**
 * One library, drawn the way AboutLibraries' M3 list draws one: the name with its version beside
 * it, a quieter line of metadata under that, and the license as a chip in the license's color.
 */
@Composable
private fun LicenseCard(
    artifact: LicenseArtifact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = SurfaceShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = artifact.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = artifact.version,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
            }
            Text(
                text = artifact.module,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LicenseChips(artifact)
        }
    }
}

/** The licenses a library declares, each as a pill in the color its family goes by. */
@Composable
private fun LicenseChips(artifact: LicenseArtifact) {
    if (artifact.licenseNames.isEmpty()) {
        Text(
            text = stringResource(R.string.licenses_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        artifact.spdxLicenses.forEach { license ->
            LicenseChip(label = license.label, color = licenseColor(license.identifier))
        }
        artifact.unknownLicenses.forEach { license ->
            val label = license.name ?: license.url
            if (label != null) {
                LicenseChip(label = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LicenseChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The color a license family goes by, taken from the scheme the reader is on rather than named
 * outright, so the chips follow the palette the rest of the app is drawn in.
 */
@Composable
@ReadOnlyComposable
private fun licenseColor(identifier: String): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        identifier.contains("GPL") -> scheme.error
        identifier == "Apache-2.0" -> scheme.primary
        identifier == "MIT" || identifier == "ISC" -> scheme.tertiary
        identifier.startsWith("BSD") || identifier.startsWith("MPL") -> scheme.secondary
        else -> scheme.onSurfaceVariant
    }
}
