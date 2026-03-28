package com.prslc.zhiflow.ui.component.comment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.viewmodel.CommentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    viewModel: CommentViewModel,
    answerId: String,
    showComments: Boolean,
    onDismissRequest: () -> Unit,
) {
    val uiState = viewModel.uiState
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    if (showComments) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.comment_count, uiState.totalCount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismissRequest()
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "close")
                    }
                }

                // list
                CommentList(
                    comments = viewModel.uiState.comments,
                    isLoading = viewModel.uiState.isLoading,
                    hasMore = viewModel.uiState.hasMore,
                    onLoadMore = { viewModel.loadComments(answerId) },
                    onAuthorClick = { /* TODO */ },
                    onLikeClick = { /* TODO */ },
                    onImageClick = { /* TODO */ }
                )
            }
        }
    }
}