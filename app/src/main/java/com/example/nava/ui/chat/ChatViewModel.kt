package com.example.nava.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.data.chat.CachedChatMessageDao
import com.example.nava.data.chat.CachedChatMessageEntity
import com.example.nava.domain.catalog.HomeTrack
import com.example.nava.data.catalog.toPublicCoverUrl
import com.example.nava.ui.social.SocialPerson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class ChatConversation(
    val id: String,
    val peerId: String,
    val peerName: String,
    val lastMessage: String?,
    val unreadCount: Long,
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val body: String?,
    val sharedTrackId: String?,
    val sharedTrackTitle: String?,
    val sharedTrackArtist: String?,
    val sharedTrackCoverUrl: String?,
    val createdAt: String,
    val status: ChatMessageStatus,
    val isMine: Boolean,
)

enum class ChatMessageStatus { Sending, Sent, Delivered, Read, Failed }

data class ChatUiState(
    val conversations: List<ChatConversation> = emptyList(),
    val activeConversation: ChatConversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val loading: Boolean = false,
    val sending: Boolean = false,
    val error: Boolean = false,
    val typingName: String? = null,
    val offline: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val cachedMessages: CachedChatMessageDao,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()
    private var inboxChannel: RealtimeChannel? = null
    private var inboxRealtimeJob: Job? = null
    private var inboxSyncJob: Job? = null
    private var inboxRefreshJob: Job? = null
    private var activeChannel: RealtimeChannel? = null
    private var liveMessagesJob: Job? = null
    private var messageBroadcastJob: Job? = null
    private var typingBroadcastJob: Job? = null
    private var messageSyncJob: Job? = null
    private var receiptJob: Job? = null
    private var typingResetJob: Job? = null
    private var remoteTypingResetJob: Job? = null
    private val messageLoadMutex = Mutex()
    private val sharedTracks = mutableMapOf<String, HomeTrack>()

    init {
        refreshInbox()
        startInboxUpdates()
    }

    fun refreshInbox() {
        inboxRefreshJob?.cancel()
        inboxRefreshJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = _state.value.activeConversation == null && _state.value.conversations.isEmpty(),
                error = false,
            )
            runCatching {
                supabase.postgrest.rpc("list_conversations", buildJsonObject { put("p_limit", 30) })
                    .decodeList<ConversationDto>()
                    .map { it.toConversation() }
            }.onSuccess { conversations ->
                _state.value = _state.value.copy(conversations = conversations, loading = false)
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = true)
            }
        }
    }

    fun open(person: SocialPerson) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = false)
        runCatching {
            val conversationId = supabase.postgrest.rpc(
                "get_or_create_direct_conversation",
                buildJsonObject { put("p_other_user_id", person.id) },
            ).decodeAs<String>()
            ChatConversation(conversationId, person.id, person.displayName, null, 0)
        }.onSuccess(::openConversation).onFailure {
            _state.value = _state.value.copy(loading = false, error = true)
        }
    }

    fun openConversation(conversation: ChatConversation) = viewModelScope.launch {
        stopRealtime()
        _state.value = _state.value.copy(activeConversation = conversation, messages = emptyList(), loading = true, error = false)
        loadMessages(conversation.id)
        runCatching { startRealtime(conversation) }
            .onFailure { _state.value = _state.value.copy(error = true) }
    }

    fun closeConversation() {
        viewModelScope.launch { stopRealtime() }
        _state.value = _state.value.copy(activeConversation = null, messages = emptyList(), draft = "")
        refreshInbox()
    }

    fun changeDraft(value: String) {
        _state.value = _state.value.copy(draft = value.take(MAX_MESSAGE_LENGTH))
        updateTyping(value.isNotBlank())
        typingResetJob?.cancel()
        if (value.isNotBlank()) {
            typingResetJob = viewModelScope.launch {
                delay(TYPING_IDLE_MS)
                updateTyping(false)
            }
        }
    }

    fun sendText() {
        val conversation = _state.value.activeConversation ?: return
        val body = _state.value.draft.trim()
        if (body.isEmpty() || _state.value.sending) return
        send(conversation.id, body, null, null)
    }

    fun shareTrack(track: HomeTrack) {
        val conversation = _state.value.activeConversation ?: return
        if (_state.value.sending) return
        send(conversation.id, null, track.id, track)
    }

    fun shareTrack(conversation: ChatConversation, track: HomeTrack, onShared: () -> Unit) {
        if (_state.value.sending) return
        send(conversation.id, null, track.id, track, onShared)
    }

    fun playSharedTrack(trackId: String, onTrackReady: (HomeTrack) -> Unit) = viewModelScope.launch {
        runCatching {
            resolveSharedTrack(trackId)
        }.onSuccess(onTrackReady).onFailure {
            _state.value = _state.value.copy(error = true)
        }
    }

    fun retry() {
        _state.value.activeConversation?.let { openConversation(it) } ?: refreshInbox()
    }

    fun dismissError() { _state.value = _state.value.copy(error = false) }

    private fun send(
        conversationId: String,
        body: String?,
        trackId: String?,
        track: HomeTrack?,
        onSuccess: () -> Unit = {},
    ) = viewModelScope.launch {
        val currentUser = supabase.auth.currentUserOrNull()
        val pendingId = "pending:${UUID.randomUUID()}"
        val pendingMessage = currentUser
            ?.takeIf { _state.value.activeConversation?.id == conversationId }
            ?.let { user ->
                ChatMessage(
                    id = pendingId,
                    senderId = user.id,
                    senderName = user.email ?: user.id,
                    body = body,
                    sharedTrackId = trackId,
                    sharedTrackTitle = track?.title,
                    sharedTrackArtist = track?.artistName,
                    sharedTrackCoverUrl = track?.coverImageUrl,
                    createdAt = Instant.now().toString(),
                    status = ChatMessageStatus.Sending,
                    isMine = true,
                )
            }
        _state.value = _state.value.copy(
            draft = if (body != null) "" else _state.value.draft,
            messages = pendingMessage?.let { _state.value.messages + it } ?: _state.value.messages,
            sending = true,
            error = false,
        )
        runCatching {
            supabase.postgrest.rpc(
                "send_conversation_message",
                buildJsonObject {
                    put("p_conversation_id", conversationId)
                    body?.let { put("p_body", it) }
                    trackId?.let { put("p_track_id", it) }
                },
            )
        }.onSuccess {
            _state.value = _state.value.copy(sending = false)
            updateTyping(false)
            runCatching {
                activeChannel?.broadcast(
                    MESSAGE_EVENT,
                    MessageEvent(currentUser?.id.orEmpty()),
                )
            }
            if (_state.value.activeConversation?.id == conversationId) {
                loadMessages(conversationId)
            } else {
                refreshInbox()
            }
            onSuccess()
        }.onFailure {
            _state.value = _state.value.copy(
                messages = _state.value.messages.map { message ->
                    if (message.id == pendingId) message.copy(status = ChatMessageStatus.Failed) else message
                },
                sending = false,
                error = true,
            )
        }
    }

    private suspend fun loadMessages(conversationId: String) = messageLoadMutex.withLock {
        if (_state.value.activeConversation?.id != conversationId) return@withLock
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return failLoading()
        try {
            val dtos = supabase.postgrest.rpc(
                "get_conversation_messages",
                buildJsonObject { put("p_conversation_id", conversationId); put("p_limit", PAGE_SIZE) },
            ).decodeList<MessageDto>()
                .asReversed()
            val coverUrls = dtos.mapNotNull(MessageDto::trackId).distinct().associateWith { trackId ->
                runCatching { resolveSharedTrack(trackId).coverImageUrl }.getOrNull()
            }
            val messages = dtos.map { it.toMessage(currentUserId, coverUrls[it.trackId]) }
            if (_state.value.activeConversation?.id != conversationId) return@withLock
            _state.value = _state.value.copy(messages = messages, loading = false, offline = false)
            cacheMessages(currentUserId, conversationId, messages)
            if (messages.any { !it.isMine && it.status != ChatMessageStatus.Read }) markRead(conversationId)
        } catch (_: Throwable) {
            val localMessages = cachedMessages.getConversation(currentUserId, conversationId).map { it.toMessage() }
            _state.value = _state.value.copy(
                messages = localMessages,
                loading = false,
                offline = localMessages.isNotEmpty(),
                error = localMessages.isEmpty(),
            )
        }
    }

    private fun markRead(conversationId: String) = viewModelScope.launch {
        runCatching {
            supabase.postgrest.rpc("mark_conversation_delivered", buildJsonObject { put("p_conversation_id", conversationId) })
            supabase.postgrest.rpc("mark_conversation_read", buildJsonObject { put("p_conversation_id", conversationId) })
            activeChannel?.broadcast(RECEIPT_EVENT, ReceiptEvent(supabase.auth.currentUserOrNull()?.id.orEmpty()))
        }.onSuccess { refreshInbox() }
    }

    private fun failLoading() { _state.value = _state.value.copy(loading = false, error = true) }

    private suspend fun startRealtime(conversation: ChatConversation) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return
        val channel = supabase.channel("conversation:${conversation.id}") {
            isPrivate = true
            broadcast { acknowledgeBroadcasts = true }
        }
        activeChannel = channel
        liveMessagesJob = viewModelScope.launch {
            channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "conversation_messages"
                filter("conversation_id", FilterOperator.EQ, conversation.id)
            }.collect {
                loadMessages(conversation.id)
                refreshInbox()
            }
        }
        messageBroadcastJob = viewModelScope.launch {
            channel.broadcastFlow<MessageEvent>(MESSAGE_EVENT).collect { event ->
                if (event.userId != currentUser.id) {
                    loadMessages(conversation.id)
                    refreshInbox()
                }
            }
        }
        typingBroadcastJob = viewModelScope.launch {
            channel.broadcastFlow<TypingEvent>(TYPING_EVENT).collect { event ->
                if (event.userId != currentUser.id) {
                    remoteTypingResetJob?.cancel()
                    _state.value = _state.value.copy(
                        typingName = if (event.isTyping) conversation.peerName else null,
                    )
                    if (event.isTyping) {
                        remoteTypingResetJob = viewModelScope.launch {
                            delay(REMOTE_TYPING_TIMEOUT_MS)
                            if (_state.value.activeConversation?.id == conversation.id) {
                                _state.value = _state.value.copy(typingName = null)
                            }
                        }
                    }
                }
            }
        }
        receiptJob = viewModelScope.launch {
            channel.broadcastFlow<ReceiptEvent>(RECEIPT_EVENT).collect { receipt ->
                if (receipt.userId != currentUser.id) loadMessages(conversation.id)
            }
        }
        messageSyncJob = viewModelScope.launch {
            while (isActive && _state.value.activeConversation?.id == conversation.id) {
                delay(MESSAGE_SYNC_FALLBACK_MS)
                loadMessages(conversation.id)
            }
        }
        channel.subscribe(blockUntilSubscribed = true)
        markRead(conversation.id)
    }

    private fun startInboxUpdates() = viewModelScope.launch {
        val currentUser = supabase.auth.currentUserOrNull() ?: return@launch
        runCatching {
            val channel = supabase.channel("conversation-inbox:${currentUser.id}")
            inboxChannel = channel
            inboxRealtimeJob = viewModelScope.launch {
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "conversation_messages"
                }.collect { refreshInbox() }
            }
            channel.subscribe(blockUntilSubscribed = true)
        }
        inboxSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(INBOX_SYNC_FALLBACK_MS)
                refreshInbox()
            }
        }
    }

    private fun updateTyping(isTyping: Boolean) = viewModelScope.launch {
        _state.value.activeConversation ?: return@launch
        val user = supabase.auth.currentUserOrNull() ?: return@launch
        runCatching {
            activeChannel
                ?.takeIf { it.status.value == RealtimeChannel.Status.SUBSCRIBED }
                ?.broadcast(
                    TYPING_EVENT,
                    TypingEvent(user.id, user.email ?: user.id, isTyping),
                )
        }
    }

    private suspend fun stopRealtime() {
        typingResetJob?.cancel()
        remoteTypingResetJob?.cancel()
        liveMessagesJob?.cancel()
        messageBroadcastJob?.cancel()
        typingBroadcastJob?.cancel()
        messageSyncJob?.cancel()
        receiptJob?.cancel()
        activeChannel?.let { supabase.realtime.removeChannel(it) }
        activeChannel = null
        _state.value = _state.value.copy(typingName = null)
    }

    override fun onCleared() {
        inboxRealtimeJob?.cancel()
        inboxSyncJob?.cancel()
        inboxRefreshJob?.cancel()
        inboxChannel?.let { channel -> viewModelScope.launch { supabase.realtime.removeChannel(channel) } }
        viewModelScope.launch { stopRealtime() }
        super.onCleared()
    }

    @Serializable
    private data class ConversationDto(
        val id: String,
        @SerialName("peer_id") val peerId: String,
        @SerialName("peer_name") val peerName: String,
        @SerialName("last_message_body") val lastMessage: String? = null,
        @SerialName("unread_count") val unreadCount: Long = 0,
    ) {
        fun toConversation() = ChatConversation(id, peerId, peerName, lastMessage, unreadCount)
    }

    @Serializable
    private data class MessageDto(
        val id: String,
        @SerialName("sender_id") val senderId: String,
        @SerialName("sender_name") val senderName: String,
        val body: String? = null,
        @SerialName("track_id") val trackId: String? = null,
        @SerialName("track_title") val trackTitle: String? = null,
        @SerialName("track_artist_name") val trackArtistName: String? = null,
        @SerialName("created_at") val createdAt: String,
        @SerialName("delivered_at") val deliveredAt: String? = null,
        @SerialName("read_at") val readAt: String? = null,
    ) {
        fun toMessage(currentUserId: String, coverUrl: String?) = ChatMessage(
            id = id,
            senderId = senderId,
            senderName = senderName,
            body = body,
            sharedTrackId = trackId,
            sharedTrackTitle = trackTitle,
            sharedTrackArtist = trackArtistName,
            sharedTrackCoverUrl = coverUrl,
            createdAt = createdAt,
            status = when {
                readAt != null -> ChatMessageStatus.Read
                deliveredAt != null -> ChatMessageStatus.Delivered
                else -> ChatMessageStatus.Sent
            },
            isMine = senderId == currentUserId,
        )
    }

    @Serializable
    private data class ShareableTrackDto(
        val id: String,
        val title: String,
        @SerialName("artist_name") val artistName: String,
        @SerialName("cover_image_url") val coverImageUrl: String,
        @SerialName("audio_url") val audioUrl: String,
        @SerialName("language_code") val languageCode: String,
    ) {
        fun toHomeTrack(supabase: SupabaseClient) = HomeTrack(
            id = id,
            title = title,
            artistName = artistName,
            coverImageUrl = coverImageUrl.toPublicCoverUrl(supabase),
            audioUrl = audioUrl,
            languageCode = languageCode,
        )
    }

    @Serializable
    private data class TypingEvent(
        @SerialName("user_id") val userId: String,
        @SerialName("display_name") val displayName: String,
        @SerialName("is_typing") val isTyping: Boolean,
    )

    @Serializable
    private data class MessageEvent(@SerialName("user_id") val userId: String)

    @Serializable
    private data class ReceiptEvent(@SerialName("user_id") val userId: String)

    private suspend fun resolveSharedTrack(trackId: String): HomeTrack = sharedTracks[trackId] ?: supabase.postgrest
        .rpc("get_shareable_track", buildJsonObject { put("p_track_id", trackId) })
        .decodeList<ShareableTrackDto>()
        .first()
        .toHomeTrack(supabase)
        .also { sharedTracks[trackId] = it }

    private suspend fun cacheMessages(accountId: String, conversationId: String, messages: List<ChatMessage>) {
        cachedMessages.upsertAll(messages.filterNot { it.status == ChatMessageStatus.Sending }.map { message ->
            CachedChatMessageEntity(
                cacheId = "$accountId:${message.id}",
                accountId = accountId,
                conversationId = conversationId,
                messageId = message.id,
                senderId = message.senderId,
                senderName = message.senderName,
                body = message.body,
                sharedTrackId = message.sharedTrackId,
                sharedTrackTitle = message.sharedTrackTitle,
                sharedTrackArtist = message.sharedTrackArtist,
                sharedTrackCoverUrl = message.sharedTrackCoverUrl,
                createdAt = message.createdAt,
                status = message.status.name,
                isMine = message.isMine,
            )
        })
    }

    private fun CachedChatMessageEntity.toMessage() = ChatMessage(
        id = messageId,
        senderId = senderId,
        senderName = senderName,
        body = body,
        sharedTrackId = sharedTrackId,
        sharedTrackTitle = sharedTrackTitle,
        sharedTrackArtist = sharedTrackArtist,
        sharedTrackCoverUrl = sharedTrackCoverUrl,
        createdAt = createdAt,
        status = runCatching { ChatMessageStatus.valueOf(status) }.getOrDefault(ChatMessageStatus.Sent),
        isMine = isMine,
    )

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_MESSAGE_LENGTH = 2_000
        const val TYPING_IDLE_MS = 1_200L
        const val REMOTE_TYPING_TIMEOUT_MS = 4_000L
        const val MESSAGE_SYNC_FALLBACK_MS = 2_000L
        const val INBOX_SYNC_FALLBACK_MS = 5_000L
        const val RECEIPT_EVENT = "message-receipt"
        const val MESSAGE_EVENT = "message-changed"
        const val TYPING_EVENT = "typing-changed"
    }
}
