package com.example.nava.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.domain.catalog.HomeTrack
import com.example.nava.ui.social.SocialPerson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    val createdAt: String,
    val delivered: Boolean,
    val read: Boolean,
    val isMine: Boolean,
)

data class ChatUiState(
    val conversations: List<ChatConversation> = emptyList(),
    val activeConversation: ChatConversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val loading: Boolean = false,
    val sending: Boolean = false,
    val error: Boolean = false,
    val typingName: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(private val supabase: SupabaseClient) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()
    private var activeChannel: RealtimeChannel? = null
    private var liveMessagesJob: Job? = null
    private var presenceJob: Job? = null
    private var typingResetJob: Job? = null

    init { refreshInbox() }

    fun refreshInbox() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = false)
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
        send(conversation.id, body, null)
    }

    fun shareTrack(track: HomeTrack) {
        val conversation = _state.value.activeConversation ?: return
        if (_state.value.sending) return
        send(conversation.id, null, track.id)
    }

    fun playSharedTrack(trackId: String, onTrackReady: (HomeTrack) -> Unit) = viewModelScope.launch {
        runCatching {
            supabase.postgrest.rpc("get_shareable_track", buildJsonObject { put("p_track_id", trackId) })
                .decodeList<ShareableTrackDto>()
                .first()
                .toHomeTrack()
        }.onSuccess(onTrackReady).onFailure {
            _state.value = _state.value.copy(error = true)
        }
    }

    fun retry() {
        _state.value.activeConversation?.let { openConversation(it) } ?: refreshInbox()
    }

    fun dismissError() { _state.value = _state.value.copy(error = false) }

    private fun send(conversationId: String, body: String?, trackId: String?) = viewModelScope.launch {
        _state.value = _state.value.copy(sending = true, error = false)
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
            _state.value = _state.value.copy(draft = "", sending = false)
            updateTyping(false)
            loadMessages(conversationId)
        }.onFailure {
            _state.value = _state.value.copy(sending = false, error = true)
        }
    }

    private suspend fun loadMessages(conversationId: String) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return failLoading()
        runCatching {
            supabase.postgrest.rpc(
                "get_conversation_messages",
                buildJsonObject { put("p_conversation_id", conversationId); put("p_limit", PAGE_SIZE) },
            ).decodeList<MessageDto>()
                .asReversed()
                .map { it.toMessage(currentUserId) }
        }.onSuccess { messages ->
            _state.value = _state.value.copy(messages = messages, loading = false)
            markRead(conversationId)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, error = true)
        }
    }

    private fun markRead(conversationId: String) = viewModelScope.launch {
        runCatching {
            supabase.postgrest.rpc("mark_conversation_delivered", buildJsonObject { put("p_conversation_id", conversationId) })
            supabase.postgrest.rpc("mark_conversation_read", buildJsonObject { put("p_conversation_id", conversationId) })
        }
    }

    private fun failLoading() { _state.value = _state.value.copy(loading = false, error = true) }

    private suspend fun startRealtime(conversation: ChatConversation) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return
        val channel = supabase.channel("conversation:${conversation.id}") {
            isPrivate = true
            presence { key = currentUser.id }
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
        presenceJob = viewModelScope.launch {
            channel.presenceDataFlow<TypingPresence>().collect { presences ->
                val typingUser = presences.firstOrNull { it.userId != currentUser.id && it.isTyping }
                _state.value = _state.value.copy(typingName = typingUser?.displayName)
            }
        }
        channel.subscribe(blockUntilSubscribed = true)
        channel.track(TypingPresence(currentUser.id, currentUser.email ?: currentUser.id, false))
    }

    private fun updateTyping(isTyping: Boolean) = viewModelScope.launch {
        val conversation = _state.value.activeConversation ?: return@launch
        val user = supabase.auth.currentUserOrNull() ?: return@launch
        runCatching {
            activeChannel
                ?.takeIf { it.status.value == RealtimeChannel.Status.SUBSCRIBED }
                ?.track(TypingPresence(user.id, user.email ?: user.id, isTyping))
        }
    }

    private suspend fun stopRealtime() {
        typingResetJob?.cancel()
        liveMessagesJob?.cancel()
        presenceJob?.cancel()
        activeChannel?.let { supabase.realtime.removeChannel(it) }
        activeChannel = null
        _state.value = _state.value.copy(typingName = null)
    }

    override fun onCleared() {
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
        fun toMessage(currentUserId: String) = ChatMessage(
            id, senderId, senderName, body, trackId, trackTitle, trackArtistName,
            createdAt, deliveredAt != null, readAt != null, senderId == currentUserId,
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
        fun toHomeTrack() = HomeTrack(id, title, artistName, coverImageUrl, audioUrl, languageCode)
    }

    @Serializable
    private data class TypingPresence(
        @SerialName("user_id") val userId: String,
        @SerialName("display_name") val displayName: String,
        @SerialName("is_typing") val isTyping: Boolean,
    )

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_MESSAGE_LENGTH = 2_000
        const val TYPING_IDLE_MS = 1_200L
    }
}
