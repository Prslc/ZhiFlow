package com.prslc.zhiflow.ui.page.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
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
import com.prslc.zhiflow.core.utils.platform.rememberCopyTextToClipboard
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
    val isSelecting = uiState.isSelecting

    LaunchedEffect(peerId) { viewModel.load(peerId) }

    val snackbarHostState = rememberActionErrorHost(
        viewModel.actionError,
        viewModel::consumeActionError,
    )

    // The scroll is the page's to make, an animation needing the frame clock a view model's scope
    // has not got, and it waits a frame so that the message is measured by the time it is worked out.
    val messageSent = viewModel.messageSent
    LaunchedEffect(messageSent) {
        if (!messageSent) return@LaunchedEffect

        withFrameNanos { }
        viewModel.listState.animateScrollToItem(0)

        // After the scroll: the flag is this effect's own key, and clearing it first would cancel
        // the effect before it scrolled.
        viewModel.consumeSent()
    }

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
                        state = viewModel.listState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                            // The page is drawn from the bottom, and a reversed list stacks an
                            // item's own children from the bottom too: the bubble is written first
                            // so that the stamp above it comes out above it.
                            MessageBubble(
                                message = message,
                                isSelecting = isSelecting,
                                isSelected = message.id in uiState.selectedIds,
                                onLongClick = {
                                    if (isSelecting) {
                                        viewModel.toggleSelected(message.id)
                                    } else {
                                        viewModel.openMenu(message)
                                    }
                                },
                                onClick = {
                                    if (isSelecting) viewModel.toggleSelected(message.id)
                                },
                                actions = {
                                    MessageActionsMenu(
                                        message = message,
                                        expanded = viewModel.menuMessage?.id == message.id,
                                        onDismissRequest = viewModel::closeMenu,
                                        onDelete = viewModel::deleteMenuMessage,
                                        onSelect = viewModel::startSelecting,
                                    )
                                },
                            )

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

            // Picking messages replaces the box while it lasts: the page is not being written to,
            // and what the reader can do with what they picked is what belongs down here.
            if (isSelecting) {
                SelectionBar(
                    count = uiState.selectedIds.size,
                    isDeleting = uiState.isDeleting,
                    onDelete = viewModel::deleteSelected,
                    onCancel = viewModel::cancelSelecting,
                )
            } else if (peer?.isForbidden == true) {
                // An account that takes no messages is told so where the box would be, rather than
                // being offered one that cannot send.
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

/** What stands in for the box while the page is being read as a list of messages to pick from. */
@Composable
private fun SelectionBar(
    count: Int,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel) {
            Icon(
                Icons.Filled.Close,
                stringResource(android.R.string.cancel),
            )
        }

        Text(
            text = stringResource(R.string.chat_selection_count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        FilledTonalButton(
            onClick = onDelete,
            enabled = count > 0 && !isDeleting,
        ) {
            Text(stringResource(R.string.chat_message_delete))
        }
    }
}

/**
 * What can be done with one message, in a panel that hangs off the message itself.
 *
 * A conversation asks for something that stays out of the way of the one being answered: the
 * entries are few, so they go beside the message rather than over the page, and the panel leaving
 * is the reader's tap anywhere else rather than an entry of its own.
 *
 * @param message The message the reader is holding.
 * @param expanded Whether the panel is showing.
 * @param onDismissRequest Called when the reader taps away from it.
 * @param onDelete Called when they delete the message, which is a delete from this side only.
 * @param onSelect Called when they ask for the page to be read as a list of things to pick from.
 */
@Composable
private fun MessageActionsMenu(
    message: ChatMessageDto,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        val copy = rememberCopyTextToClipboard()
        val menuColors = MenuDefaults.itemColors()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.chat_message_copy)) },
            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
            colors = menuColors,
            onClick = {
                copy(message.text.text)
                onDismissRequest()
            },
        )

        DropdownMenuItem(
            text = { Text(stringResource(R.string.chat_message_select)) },
            leadingIcon = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
            colors = menuColors,
            onClick = onSelect,
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.chat_message_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            colors = MenuDefaults.itemColors(
                leadingIconColor = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error,
            ),
            onClick = onDelete,
        )
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

/**
 * One message, on the side it came from, with the corner nearest that side squared off.
 *
 * @param message The message to draw.
 * @param isSelecting Whether the page is being read as a list of messages to pick from.
 * @param isSelected Whether this one has been picked.
 * @param onLongClick Called when the reader holds the bubble down.
 * @param onClick Called when they tap it while picking.
 * @param modifier Applied to the row.
 * @param actions The panel of things that can be done with this message, hung off the bubble.
 */
@Composable
private fun MessageBubble(
    message: ChatMessageDto,
    isSelecting: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val mine = message.isMine

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The mark goes on the side the bubble is not, so that being picked does not move it.
        if (mine) {
            SelectionMark(isSelecting = isSelecting, isSelected = isSelected)
        }

        Box {
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
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    // No ripple: a bubble has nothing to open, and one on every tap would say it
                    // did. Picking is the one thing a tap means, and it shows in the mark.
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ),
            ) {
                // The server writes a message as html, links and all; the links it carries are the
                // point of most of them, so they are drawn tappable rather than as text.
                ZRichText(
                    content = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }

            actions()
        }

        if (!mine) {
            SelectionMark(isSelecting = isSelecting, isSelected = isSelected)
        }
    }
}

/** The circle one message is picked out by, when the page is being read as a list. */
@Composable
private fun SelectionMark(
    isSelecting: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isSelecting) return

    Icon(
        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
        contentDescription = null,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .size(22.dp),
        tint = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
    )
}
