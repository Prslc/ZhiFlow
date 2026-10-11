package com.prslc.zhiflow.ui.page.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.data.dto.ChatMessageDto
import com.prslc.zhiflow.data.dto.ChatPeerDto
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.component.richtext.ZRichText
import org.koin.androidx.compose.koinViewModel

/** How long a silence has to be before the messages under it are stamped with the time. */
private const val MARK_GAP = 5 * 60L

/**
 * One conversation, read from the newest message back, with a box to answer in at the end.
 *
 * Only text is written here: the box takes what the reader types, and the server is told nothing
 * about the message beyond its words.
 *
 * @param peerId The user hash of the person at the other end.
 * @param sourceType How the page was opened, as the card that opened it said.
 * @param onBack Called by the back arrow.
 * @param modifier Applied to the scaffold.
 * @param viewModel The page's own; a caller passes one only to test.
 */
@Composable
fun ChatScreen(
    peerId: String,
    sourceType: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState
    val messages = uiState.messages
    val peer = uiState.peer
    val error = uiState.error

    LaunchedEffect(peerId) { viewModel.load(peerId) }

    val snackbarHostState = rememberActionErrorHost(
        viewModel.sendError,
        viewModel::consumeSendError,
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { PeerTitle(uiState.peer) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.general_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // The scaffold's padding is a plain padding and consumes nothing, so without this
                // the keyboard is counted on top of the navigation bar it already covers.
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty() && uiState.isLoading) {
                    LoadingView(modifier = Modifier.fillMaxSize())
                } else if (messages.isEmpty() && error != null) {
                    ErrorView(
                        message = error.uiMessage,
                        onRetry = { viewModel.load(peerId) },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (messages.isEmpty()) {
                    EmptyView(
                        message = stringResource(R.string.chat_empty),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        // Laid out from the bottom, so the newest message is where the reader left
                        // off and a short conversation sits at the end of the screen rather than
                        // the top.
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                            // The page is drawn from the bottom, and a reversed list stacks an
                            // item's own children from the bottom too: the bubble is written first
                            // so that the stamp above it comes out above it.
                            MessageBubble(message)

                            // The message sent before this one is the next in the list, and the
                            // newest always carries a stamp, being the one whose time the reader
                            // is looking for.
                            val before = messages.getOrNull(index + 1)
                            if (before == null || message.createdTime - before.createdTime > MARK_GAP) {
                                TimeMark(relativeStamp(message.createdTime))
                            }
                        }
                    }
                }
            }

            // An account that takes no messages is told so where the box would be, rather than
            // being offered one that cannot send.
            if (peer?.isForbidden == true) {
                Text(
                    text = stringResource(R.string.chat_forbidden),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
            } else {
                Composer(
                    draft = viewModel.draft,
                    isSending = uiState.isSending,
                    onDraftChange = viewModel::onDraftChange,
                    onSend = { viewModel.send(peerId, sourceType) },
                )
            }
        }
    }
}

/** The box the reader writes into, and the one button that sends what is in it. */
@Composable
private fun Composer(
    draft: String,
    isSending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = { Text(stringResource(R.string.chat_placeholder)) },
            maxLines = 4,
            shape = RoundedCornerShape(20.dp),
            // A filled field draws a line under itself; in a box the reader writes paragraphs into
            // that line reads as an underline on the text rather than as the field's edge.
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = onSend,
            enabled = draft.isNotBlank() && !isSending,
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                stringResource(R.string.chat_send),
            )
        }
    }
}

/** The time a message was sent, over the messages that follow it. */
@Composable
private fun TimeMark(stamp: String, modifier: Modifier = Modifier) {
    Text(
        text = stamp,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}

/** Who the conversation is with: their picture, and their name. */
@Composable
private fun PeerTitle(peer: ChatPeerDto?, modifier: Modifier = Modifier) {
    if (peer == null) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = peer.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = peer.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One message, on the side it came from, with the corner nearest that side squared off. */
@Composable
private fun MessageBubble(message: ChatMessageDto, modifier: Modifier = Modifier) {
    val mine = message.isMine

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (mine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (mine) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            shape = if (mine) {
                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            } else {
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
            },
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            // The server writes a message as html, links and all; the links it carries are the
            // point of most of them, so they are drawn tappable rather than as text.
            ZRichText(
                content = message.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
