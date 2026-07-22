package com.example.nava.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.nava.R
import com.example.nava.playback.PlaybackViewModel
import com.example.nava.ui.theme.NavaDimensions
import com.example.nava.ui.theme.NavaSpacing
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ChatShell(
    modifier: Modifier,
    onBack: () -> Unit,
    onConversationBack: (() -> Unit)? = null,
    viewModel: ChatViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val nowPlaying by playbackViewModel.nowPlaying.collectAsState()
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = {
                if (state.activeConversation == null) {
                    onBack()
                } else {
                    viewModel.closeConversation()
                    onConversationBack?.invoke()
                }
            }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                state.activeConversation?.peerName ?: stringResource(R.string.messages),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.activeConversation == null) {
            ConversationInbox(state, viewModel)
        } else {
            ConversationThread(
                state = state,
                onDraftChange = viewModel::changeDraft,
                onSend = viewModel::sendText,
                canShareNowPlaying = nowPlaying != null,
                onShareNowPlaying = { nowPlaying?.track?.let(viewModel::shareTrack) },
                onPlaySharedTrack = { viewModel.playSharedTrack(it, playbackViewModel::play) },
            )
        }
        if (state.error) {
            Button(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun ConversationInbox(state: ChatUiState, viewModel: ChatViewModel) {
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.conversations.isEmpty() -> Text(stringResource(R.string.messages_empty))
        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
            items(state.conversations, key = ChatConversation::id) { conversation ->
                Card(onClick = { viewModel.openConversation(conversation) }) {
                    Column(Modifier.fillMaxWidth().padding(NavaSpacing.Md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(conversation.peerName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (conversation.unreadCount > 0) Text(conversation.unreadCount.toString(), color = MaterialTheme.colorScheme.primary)
                        }
                        Text(conversation.lastMessage ?: stringResource(R.string.shared_music), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ConversationThread(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    canShareNowPlaying: Boolean,
    onShareNowPlaying: () -> Unit,
    onPlaySharedTrack: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    if (state.loading) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        if (state.offline) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                ) {
                    Icon(Icons.Outlined.CloudOff, contentDescription = null)
                    Text(stringResource(R.string.chat_offline_history), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
        ) {
            items(state.messages, key = ChatMessage::id) { message ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(.84f),
                        shape = MaterialTheme.shapes.large,
                        color = if (message.isMine) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (message.isMine) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    ) {
                        Column(Modifier.padding(NavaSpacing.Md), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                            if (!message.isMine) {
                                Text(
                                    message.senderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            message.body?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                            message.sharedTrackId?.let { trackId ->
                                SharedTrackCard(message = message, onClick = { onPlaySharedTrack(trackId) })
                            }
                            MessageMetadata(message)
                        }
                    }
                }
            }
        }
        state.typingName?.let { typingName ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    stringResource(R.string.typing, typingName),
                    modifier = Modifier.padding(horizontal = NavaSpacing.Md, vertical = NavaSpacing.Sm),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = state.draft,
            onValueChange = onDraftChange,
            label = { Text(stringResource(R.string.message_hint)) },
            enabled = !state.sending,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onShareNowPlaying, enabled = canShareNowPlaying && !state.sending) {
            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_now_playing))
        }
        IconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.sending) {
            Icon(Icons.Outlined.Send, contentDescription = stringResource(R.string.send_message))
        }
    }
}

@Composable
private fun SharedTrackCard(message: ChatMessage, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Box {
                AsyncImage(
                    model = message.sharedTrackCoverUrl,
                    contentDescription = stringResource(
                        R.string.track_artwork,
                        message.sharedTrackTitle ?: stringResource(R.string.shared_music),
                    ),
                    contentScale = ContentScale.Crop,
                    fallback = painterResource(R.drawable.ic_launcher_foreground),
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    modifier = Modifier
                        .size(NavaDimensions.ChatTrackArtworkSize)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(NavaDimensions.ChatTrackPlayBadgeSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.padding(NavaSpacing.Xs))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(
                    message.sharedTrackTitle ?: stringResource(R.string.shared_music),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                )
                message.sharedTrackArtist?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(stringResource(R.string.tap_to_play_track), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MessageMetadata(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatChatTimestamp(message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (message.isMine) {
            val icon = when (message.status) {
                ChatMessageStatus.Sending -> Icons.Outlined.Schedule
                ChatMessageStatus.Sent, ChatMessageStatus.Delivered -> Icons.Outlined.Done
                ChatMessageStatus.Read -> Icons.Outlined.DoneAll
                ChatMessageStatus.Failed -> Icons.Outlined.ErrorOutline
            }
            val description = when (message.status) {
                ChatMessageStatus.Sending -> R.string.message_sending
                ChatMessageStatus.Sent -> R.string.message_sent
                ChatMessageStatus.Delivered -> R.string.message_delivered
                ChatMessageStatus.Read -> R.string.message_read
                ChatMessageStatus.Failed -> R.string.message_failed
            }
            Icon(
                icon,
                contentDescription = stringResource(description),
                modifier = Modifier.padding(start = NavaSpacing.Xs).size(NavaDimensions.ChatReceiptIconSize),
                tint = when (message.status) {
                    ChatMessageStatus.Read -> MaterialTheme.colorScheme.primary
                    ChatMessageStatus.Failed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

private fun formatChatTimestamp(value: String): String = runCatching {
    OffsetDateTime.parse(value)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}.recoverCatching {
    Instant.parse(value)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}.getOrElse { value.take(5) }
