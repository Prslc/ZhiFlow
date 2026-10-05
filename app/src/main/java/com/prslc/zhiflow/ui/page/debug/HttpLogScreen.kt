package com.prslc.zhiflow.ui.page.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.network.HttpClientProvider
import com.prslc.zhiflow.core.network.HttpLogEntry
import com.prslc.zhiflow.core.utils.platform.rememberCopyTextToClipboard
import com.prslc.zhiflow.ui.component.common.ContentTypeLabel
import com.prslc.zhiflow.ui.component.common.EmptyView
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
private const val SUMMARY_MAX_CHARS = 120

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpLogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HttpLogViewModel = koinViewModel(),
) {
    val entries = viewModel.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.http_log_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.general_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::clear,
                        enabled = entries.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            stringResource(R.string.http_log_action_clear),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!viewModel.loggingEnabled) {
                CaptureOffHint()
            }

            if (entries.isEmpty()) {
                EmptyView(
                    message = stringResource(R.string.http_log_empty),
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        HttpLogItem(entry)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureOffHint() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.http_log_disabled),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun HttpLogItem(entry: HttpLogEntry) {
    var expanded by remember { mutableStateOf(false) }
    val time = remember(entry.at) {
        Instant.ofEpochMilli(entry.at).atZone(ZoneId.systemDefault()).format(TIME_FORMATTER)
    }
    val path = remember(entry.url) {
        runCatching {
            entry.url.toHttpUrl().let { url ->
                url.encodedPath + url.encodedQuery?.let { "?$it" }.orEmpty()
            }
        }.getOrDefault(entry.url)
    }
    val failure = remember(entry) { failureSummary(entry) }

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(entry)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.method,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.http_log_duration, entry.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = path,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (failure != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = failure,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                BodySection(
                    title = stringResource(R.string.http_log_section_request),
                    body = entry.requestBody,
                    truncated = false,
                )
                BodySection(
                    title = stringResource(R.string.http_log_section_response),
                    body = entry.responseBody,
                    truncated = entry.truncated,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(entry: HttpLogEntry) {
    val scheme = MaterialTheme.colorScheme
    val status = entry.status
    val (container, content) = when (status) {
        null -> scheme.error to scheme.onError
        in 200..299 -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        else -> scheme.errorContainer to scheme.onErrorContainer
    }

    ContentTypeLabel(
        text = status?.toString() ?: stringResource(R.string.http_log_status_error),
        containerColor = container,
        contentColor = content,
    )
}

@Composable
private fun BodySection(title: String, body: String?, truncated: Boolean) {
    if (body == null) return
    val copy = rememberCopyTextToClipboard()

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (truncated) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.http_log_truncated),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { copy(body) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.http_log_action_copy),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SelectionContainer {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

/** The server's own explanation of a failure, pulled out of the error JSON when possible. */
private fun failureSummary(entry: HttpLogEntry): String? {
    val status = entry.status
    if (status != null && status in 200..299) return null
    entry.error?.let { return it }

    val body = entry.responseBody ?: return null
    val message = runCatching {
        val root = HttpClientProvider.jsonInstance.parseToJsonElement(body).jsonObject
        (root["error"]?.jsonObject?.get("message") ?: root["message"])?.jsonPrimitive?.content
    }.getOrNull()

    return message?.takeIf { it.isNotBlank() } ?: body.take(SUMMARY_MAX_CHARS)
}
