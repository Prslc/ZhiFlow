package com.prslc.zhiflow.ui.component.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.ui.component.common.LoadMoreErrorItem
import com.prslc.zhiflow.ui.page.content.CollectionViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * The picker a reader saves content into: it loads the collections they own, lets them tick and
 * untick, and writes the lot as one request when they confirm.
 *
 * The dialog owns that request and its own list, so [onResult] is all a caller has to act on -- and
 * it only arrives when something actually changed, since the confirm button stays disabled until
 * then.
 *
 * @param id The content being placed.
 * @param contentType What that content is; the API needs it beside the id.
 * @param onDismissRequest Called when the reader backs out, cancels, or confirms.
 * @param onResult Called with whether the content sits in any collection once the change has
 *   landed, before the dialog is dismissed.
 * @param modifier Applied to the sheet's `Surface`, ahead of the `fillMaxWidth` and the vertical
 *   padding it adds itself.
 * @param viewModel The dialog loads and writes through it; a caller passes one only to test.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollectionDialog(
    id: String,
    contentType: ContentType,
    onDismissRequest: () -> Unit,
    onResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel()
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(id) {
        viewModel.loadCollections(id, contentType)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            val state = viewModel.uiState

            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                // Header
                Text(
                    text = stringResource(R.string.collection_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )

                Spacer(Modifier.height(8.dp))

                // List Area
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        state.isLoading && state.collections.isEmpty() -> {
                            LoadingIndicator()
                        }

                        state.collections.isEmpty() && state.error != null -> {
                            LoadMoreErrorItem(
                                message = state.error.uiMessage,
                                onRetry = { viewModel.loadCollections(id, contentType) },
                            )
                        }

                        state.collections.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.collection_item_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }

                        else -> {
                            LazyColumn {
                                items(
                                    items = state.collections,
                                    key = { it.id }
                                ) { collection ->
                                    CollectionItem(
                                        title = collection.title,
                                        itemCount = collection.itemCount,
                                        isPublic = collection.isPublic,
                                        isDefault = collection.isDefault,
                                        isSelected = state.selectedIds[collection.id] == true,
                                        onToggle = {
                                            if (!state.isLoading) {
                                                viewModel.toggleSelection(collection.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Only a submit failure reaches here; a failed initial load shows
                // LoadMoreErrorItem.
                val submitError = state.error?.takeIf { state.collections.isNotEmpty() }
                LaunchedEffect(submitError) {
                    if (submitError != null) haptic.performHapticFeedback(HapticFeedbackType.Reject)
                }

                if (submitError != null) {
                    Text(
                        text = submitError.uiMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(android.R.string.cancel))
                    }

                    Spacer(Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            viewModel.updateCollectionStatus(id, contentType) { isFavorite ->
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onResult(isFavorite)
                                onDismissRequest()
                            }
                        },
                        enabled = !state.isLoading && viewModel.hasChanges()
                    ) {
                        if (state.isLoading && state.collections.isNotEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = stringResource(android.R.string.ok),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One row of the collection picker.
 *
 * @param title The collection's name.
 * @param itemCount How many contents it holds, drawn under the name.
 * @param isPublic False draws a lock beside the name.
 * @param isDefault True draws the chip that marks the reader's default collection.
 * @param isSelected The checkbox's state; the row holds none of its own.
 * @param modifier Applied to the row, ahead of the tap target and the padding it adds itself.
 * @param onToggle Called by a tap on the row or on the checkbox.
 */
@Composable
fun CollectionItem(
    title: String,
    itemCount: Int,
    isPublic: Boolean,
    isDefault: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )

                // default
                if (isDefault) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = stringResource(R.string.collection_default_label),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                if (!isPublic) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.collection_private_desc),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Text(
                text = pluralStringResource(R.plurals.collection_item_count, itemCount, itemCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
        )
    }
}
