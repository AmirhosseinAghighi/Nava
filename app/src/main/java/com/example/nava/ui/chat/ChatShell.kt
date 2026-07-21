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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.nava.R
import com.example.nava.playback.PlaybackViewModel
import com.example.nava.ui.theme.NavaSpacing

@Composable
fun ChatShell(
    modifier: Modifier,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val nowPlaying by playbackViewModel.nowPlaying.collectAsState()
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { if (state.activeConversation == null) onBack() else viewModel.closeConversation() }) {
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
    onShareNowPlaying: () -> Unit,
    onPlaySharedTrack: (String) -> Unit,
) {
    if (state.loading) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
        ) {
            items(state.messages, key = ChatMessage::id) { message ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
                ) {
                    Card {
                        Column(Modifier.padding(NavaSpacing.Md), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                            if (!message.isMine) Text(message.senderName, style = MaterialTheme.typography.labelMedium)
                            message.body?.let { Text(it) }
                            message.sharedTrackId?.let { trackId ->
                                Card(onClick = { onPlaySharedTrack(trackId) }) {
                                    Row(Modifier.padding(NavaSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.MusicNote, contentDescription = stringResource(R.string.play_shared_track))
                                        Column(Modifier.padding(start = NavaSpacing.Sm)) {
                                            Text(message.sharedTrackTitle ?: stringResource(R.string.shared_music), style = MaterialTheme.typography.titleSmall)
                                            message.sharedTrackArtist?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (message.isMine) {
                        Text(
                            stringResource(if (message.read) R.string.message_read else if (message.delivered) R.string.message_delivered else R.string.message_sent),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
        state.typingName?.let { typingName ->
            Text(stringResource(R.string.typing, typingName), style = MaterialTheme.typography.labelMedium)
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
        IconButton(onClick = onShareNowPlaying, enabled = !state.sending) {
            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_now_playing))
        }
        IconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.sending) {
            Icon(Icons.Outlined.Send, contentDescription = stringResource(R.string.send_message))
        }
    }
}
