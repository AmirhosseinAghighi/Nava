package com.example.nava.ui.chat

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.example.nava.BuildConfig
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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
    private var activeRealtimeSession: ConversationRealtimeSession? = null
    private var conversationRequestSerial = 0L
    private val realtimeLifecycleMutex = Mutex()
    private var activeMessagePagingSource: ChatMessagePagingSource? = null
    private val activeConversationId = MutableStateFlow<String?>(null)
    private val messageLoadMutex = Mutex()
    private val sharedTracks = mutableMapOf<String, HomeTrack>()

    val pagedMessages: Flow<PagingData<ChatMessage>> = activeConversationId
        .flatMapLatest { conversationId ->
            conversationId?.let {
                Pager(
                    config = PagingConfig(
                        pageSize = PAGE_SIZE,
                        initialLoadSize = PAGE_SIZE,
                        prefetchDistance = MESSAGE_PREFETCH_DISTANCE,
                        enablePlaceholders = false,
                    ),
                    pagingSourceFactory = {
                        ChatMessagePagingSource(it).also { source -> activeMessagePagingSource = source }
                    },
                ).flow
            } ?: flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

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
                _state.value = _state.value.copy(
                    loading = false,
                    error = _state.value.activeConversation == null,
                )
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

    fun openConversation(conversation: ChatConversation): Job {
        val requestSerial = ++conversationRequestSerial
        return viewModelScope.launch {
            realtimeLifecycleMutex.withLock {
                if (requestSerial != conversationRequestSerial) return@withLock
                stopRealtimeLocked()
                if (requestSerial != conversationRequestSerial) return@withLock

                activeConversationId.value = conversation.id
                _state.value = _state.value.copy(
                    activeConversation = conversation,
                    messages = emptyList(),
                    draft = "",
                    typingName = null,
                    loading = false,
                    error = false,
                    offline = false,
                )
                try {
                    startRealtime(conversation, requestSerial)
                } catch (_: Throwable) {
                    currentCoroutineContext().ensureActive()
                    stopRealtimeLocked()
                    if (requestSerial == conversationRequestSerial) {
                        _state.value = _state.value.copy(loading = false, error = true)
                    }
                }
            }
        }
    }

    fun closeConversation() {
        val requestSerial = ++conversationRequestSerial
        activeConversationId.value = null
        _state.value = _state.value.copy(
            activeConversation = null,
            messages = emptyList(),
            draft = "",
            typingName = null,
        )
        viewModelScope.launch {
            realtimeLifecycleMutex.withLock {
                if (requestSerial == conversationRequestSerial) stopRealtimeLocked()
            }
        }
        refreshInbox()
    }

    fun changeDraft(value: String) {
        val draft = value.take(MAX_MESSAGE_LENGTH)
        _state.value = _state.value.copy(draft = draft)
        val conversationId = _state.value.activeConversation?.id ?: return
        activeSessionFor(conversationId)?.typingCoordinator?.draftChanged(draft.isNotBlank())
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
        val typingCoordinator = activeSessionFor(conversationId)?.typingCoordinator
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
        typingCoordinator?.stopTypingAndFlush()
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
            _state.value = _state.value.copy(
                messages = _state.value.messages.map { message ->
                    if (message.id == pendingId) message.copy(status = ChatMessageStatus.Sent) else message
                },
                sending = false,
            )
            runCatching {
                activeSessionFor(conversationId)?.channel?.broadcast(
                    MESSAGE_EVENT,
                    MessageEvent(currentUser?.id.orEmpty()),
                )
            }
            if (_state.value.activeConversation?.id == conversationId) {
                activeMessagePagingSource?.invalidate()
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
                error = false,
            )
        }
    }

    private suspend fun loadMessages(
        conversationId: String,
        expectedSessionSerial: Long? = null,
    ) = messageLoadMutex.withLock {
        if (!isExpectedConversationSession(conversationId, expectedSessionSerial)) return@withLock
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
            if (!isExpectedConversationSession(conversationId, expectedSessionSerial)) return@withLock
            _state.value = _state.value.copy(messages = messages, loading = false, offline = false)
            cacheMessages(currentUserId, conversationId, messages)
            if (messages.any { !it.isMine && it.status != ChatMessageStatus.Read }) markRead(conversationId)
        } catch (_: Throwable) {
            currentCoroutineContext().ensureActive()
            val localMessages = cachedMessages.getConversation(currentUserId, conversationId).map { it.toMessage() }
            if (!isExpectedConversationSession(conversationId, expectedSessionSerial)) return@withLock
            _state.value = _state.value.copy(
                messages = localMessages,
                loading = false,
                offline = localMessages.isNotEmpty(),
                error = localMessages.isEmpty(),
            )
        }
    }

    private inner class ChatMessagePagingSource(
        private val conversationId: String,
    ) : PagingSource<String, ChatMessage>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, ChatMessage> {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: return LoadResult.Error(IllegalStateException("Not signed in"))
            val before = params.key
            return runCatching {
                val response = supabase.postgrest.rpc(
                    "get_conversation_messages",
                    buildJsonObject {
                        put("p_conversation_id", conversationId)
                        put("p_limit", params.loadSize.coerceAtMost(PAGE_SIZE))
                        before?.let { put("p_before", it) }
                    },
                ).decodeList<MessageDto>()
                val coverUrls = response.mapNotNull(MessageDto::trackId).distinct().associateWith { trackId ->
                    runCatching { resolveSharedTrack(trackId).coverImageUrl }.getOrNull()
                }
                val messages = response.map { it.toMessage(currentUserId, coverUrls[it.trackId]) }
                cacheMessages(currentUserId, conversationId, messages)
                if (before == null) removeConfirmedPendingMessages(conversationId, messages)
                if (before == null && messages.any { !it.isMine && it.status != ChatMessageStatus.Read }) {
                    markRead(conversationId)
                }
                LoadResult.Page(
                    data = messages,
                    prevKey = null,
                    nextKey = messages.lastOrNull()?.createdAt.takeIf { messages.size >= params.loadSize },
                )
            }.getOrElse { error ->
                if (before != null) return LoadResult.Error(error)
                val cached = cachedMessages.getConversation(currentUserId, conversationId).map { it.toMessage() }
                if (cached.isEmpty()) LoadResult.Error(error)
                else LoadResult.Page(data = cached.asReversed(), prevKey = null, nextKey = null)
            }
        }

        override fun getRefreshKey(state: PagingState<String, ChatMessage>): String? = null
    }

    private fun removeConfirmedPendingMessages(
        conversationId: String,
        confirmedMessages: List<ChatMessage>,
    ) {
        val currentState = _state.value
        if (currentState.activeConversation?.id != conversationId) return
        val confirmedPendingIds = currentState.messages
            .asSequence()
            .filter { message ->
                message.id.startsWith(PENDING_MESSAGE_ID_PREFIX) &&
                    message.status == ChatMessageStatus.Sent
            }
            .filter { pending -> confirmedMessages.any { confirmed -> confirmed.matchesPendingMessage(pending) } }
            .map(ChatMessage::id)
            .toSet()
        if (confirmedPendingIds.isNotEmpty()) {
            _state.value = currentState.copy(
                messages = currentState.messages.filterNot { it.id in confirmedPendingIds },
            )
        }
    }

    private fun ChatMessage.matchesPendingMessage(pending: ChatMessage): Boolean {
        if (!isMine || senderId != pending.senderId || body != pending.body || sharedTrackId != pending.sharedTrackId) {
            return false
        }
        val pendingCreatedAt = runCatching { Instant.parse(pending.createdAt).toEpochMilli() }.getOrNull() ?: return false
        val confirmedCreatedAt = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrNull() ?: return false
        return confirmedCreatedAt in
            (pendingCreatedAt - PENDING_MESSAGE_CLOCK_SKEW_MS)..(pendingCreatedAt + PENDING_MESSAGE_CONFIRMATION_MS)
    }

    private fun markRead(conversationId: String) = viewModelScope.launch {
        val session = activeSessionFor(conversationId)
        runCatching {
            supabase.postgrest.rpc("mark_conversation_delivered", buildJsonObject { put("p_conversation_id", conversationId) })
            supabase.postgrest.rpc("mark_conversation_read", buildJsonObject { put("p_conversation_id", conversationId) })
            session
                ?.takeIf(::isCurrentSession)
                ?.channel
                ?.broadcast(RECEIPT_EVENT, ReceiptEvent(supabase.auth.currentUserOrNull()?.id.orEmpty()))
        }.onSuccess { refreshInbox() }
    }

    private fun failLoading() { _state.value = _state.value.copy(loading = false, error = true) }

    private suspend fun startRealtime(
        conversation: ChatConversation,
        sessionSerial: Long,
    ): ConversationRealtimeSession {
        val currentUser = checkNotNull(supabase.auth.currentUserOrNull()) {
            "A signed-in user is required for conversation realtime"
        }
        val channel = supabase.channel("conversation:${conversation.id}") {
            isPrivate = true
            broadcast { acknowledgeBroadcasts = true }
        }
        val typingCoordinator = TypingCoordinator(
            sessionSerial = sessionSerial,
            channel = channel,
            event = TypingEvent(currentUser.id, currentUser.email ?: currentUser.id, false),
        )
        val session = ConversationRealtimeSession(
            serial = sessionSerial,
            conversation = conversation,
            channel = channel,
            currentUserId = currentUser.id,
            typingCoordinator = typingCoordinator,
        )
        activeRealtimeSession = session

        session.liveMessagesJob = viewModelScope.launch {
            channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "conversation_messages"
                filter("conversation_id", FilterOperator.EQ, conversation.id)
            }.collect { change ->
                if (!isCurrentSession(session)) return@collect
                val senderId = change.record["sender_id"]?.jsonPrimitive?.contentOrNull
                if (senderId != null && senderId != session.currentUserId) clearRemoteTyping(session)
                activeMessagePagingSource?.invalidate()
                refreshInbox()
            }
        }
        session.messageSyncJob = viewModelScope.launch {
            while (isActive && isCurrentSession(session)) {
                delay(MESSAGE_SYNC_FALLBACK_MS)
                if (channel.status.value != RealtimeChannel.Status.SUBSCRIBED) {
                    activeMessagePagingSource?.invalidate()
                }
            }
        }
        session.channelStatusJob = viewModelScope.launch {
            var previousStatus: RealtimeChannel.Status? = null
            channel.status.collect { status ->
                if (!isCurrentSession(session)) return@collect
                val becameSubscribed =
                    status == RealtimeChannel.Status.SUBSCRIBED &&
                        previousStatus != RealtimeChannel.Status.SUBSCRIBED
                if (status != previousStatus) {
                    debugLog("conversation session=${session.serial} connection=$status")
                }
                if (becameSubscribed) {
                    restartBroadcastCollectors(session)
                    activeMessagePagingSource?.invalidate()
                }
                session.typingCoordinator.channelStatusChanged(status)
                if (becameSubscribed) markRead(conversation.id)
                previousStatus = status
            }
        }
        channel.subscribe(blockUntilSubscribed = false)
        return session
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

    private fun activeSessionFor(conversationId: String): ConversationRealtimeSession? =
        activeRealtimeSession?.takeIf { session ->
            session.conversation.id == conversationId && isCurrentSession(session)
        }

    private fun restartBroadcastCollectors(session: ConversationRealtimeSession) {
        session.messageBroadcastJob?.cancel()
        session.typingBroadcastJob?.cancel()
        session.receiptJob?.cancel()

        session.messageBroadcastJob = viewModelScope.launch {
            session.channel.broadcastFlow<MessageEvent>(MESSAGE_EVENT).collect { event ->
                if (isCurrentSession(session) && event.userId != session.currentUserId) {
                    clearRemoteTyping(session)
                    activeMessagePagingSource?.invalidate()
                    refreshInbox()
                }
            }
        }
        session.typingBroadcastJob = viewModelScope.launch {
            session.channel.broadcastFlow<TypingEvent>(TYPING_EVENT).collect { event ->
                if (isCurrentSession(session) && event.userId != session.currentUserId) {
                    receiveRemoteTyping(session, event.isTyping)
                }
            }
        }
        session.receiptJob = viewModelScope.launch {
            session.channel.broadcastFlow<ReceiptEvent>(RECEIPT_EVENT).collect { receipt ->
                if (isCurrentSession(session) && receipt.userId != session.currentUserId) {
                    activeMessagePagingSource?.invalidate()
                }
            }
        }
    }

    private fun isCurrentSession(session: ConversationRealtimeSession): Boolean =
        activeRealtimeSession === session &&
            conversationRequestSerial == session.serial &&
            _state.value.activeConversation?.id == session.conversation.id

    private fun isExpectedConversationSession(conversationId: String, expectedSessionSerial: Long?): Boolean {
        if (_state.value.activeConversation?.id != conversationId) return false
        return expectedSessionSerial == null || activeRealtimeSession?.serial == expectedSessionSerial
    }

    private fun receiveRemoteTyping(session: ConversationRealtimeSession, isTyping: Boolean) {
        session.remoteTypingResetJob?.cancel()
        session.remoteTypingResetJob = null
        if (!isCurrentSession(session)) return
        debugLog("conversation session=${session.serial} remote-typing=$isTyping")
        _state.value = _state.value.copy(
            typingName = if (isTyping) session.conversation.peerName else null,
        )
        if (isTyping) {
            session.remoteTypingResetJob = viewModelScope.launch {
                delay(REMOTE_TYPING_TIMEOUT_MS)
                if (isCurrentSession(session)) {
                    debugLog("conversation session=${session.serial} remote-typing=false reason=timeout")
                    _state.value = _state.value.copy(typingName = null)
                }
            }
        }
    }

    private fun clearRemoteTyping(session: ConversationRealtimeSession) {
        session.remoteTypingResetJob?.cancel()
        session.remoteTypingResetJob = null
        if (isCurrentSession(session)) {
            _state.value = _state.value.copy(typingName = null)
        }
    }

    private suspend fun stopRealtimeLocked() {
        val session = activeRealtimeSession ?: return
        clearRemoteTyping(session)
        session.typingCoordinator.shutdown()
        session.liveMessagesJob?.cancel()
        session.messageBroadcastJob?.cancel()
        session.typingBroadcastJob?.cancel()
        session.messageSyncJob?.cancel()
        session.receiptJob?.cancel()
        session.channelStatusJob?.cancel()
        activeRealtimeSession = null
        try {
            supabase.realtime.removeChannel(session.channel)
        } catch (_: Throwable) {
            currentCoroutineContext().ensureActive()
            debugLog("conversation session=${session.serial} channel teardown failed")
        }
        _state.value = _state.value.copy(typingName = null)
        activeMessagePagingSource = null
    }

    override fun onCleared() {
        conversationRequestSerial++
        inboxRealtimeJob?.cancel()
        inboxSyncJob?.cancel()
        inboxRefreshJob?.cancel()
        inboxChannel?.let { channel -> viewModelScope.launch { supabase.realtime.removeChannel(channel) } }
        activeRealtimeSession?.typingCoordinator?.requestStop()
        viewModelScope.launch {
            realtimeLifecycleMutex.withLock { stopRealtimeLocked() }
        }
        super.onCleared()
    }

    private inner class ConversationRealtimeSession(
        val serial: Long,
        val conversation: ChatConversation,
        val channel: RealtimeChannel,
        val currentUserId: String,
        val typingCoordinator: TypingCoordinator,
    ) {
        var liveMessagesJob: Job? = null
        var messageBroadcastJob: Job? = null
        var typingBroadcastJob: Job? = null
        var messageSyncJob: Job? = null
        var receiptJob: Job? = null
        var channelStatusJob: Job? = null
        var remoteTypingResetJob: Job? = null
    }

    private sealed interface TypingCommand {
        data class DraftChanged(val isTyping: Boolean, val changedAtMs: Long) : TypingCommand
        data class ChannelStatusChanged(val status: RealtimeChannel.Status) : TypingCommand
        data class Stop(
            val terminate: Boolean,
            val completion: CompletableDeferred<Boolean>?,
        ) : TypingCommand
    }

    private inner class TypingCoordinator(
        private val sessionSerial: Long,
        private val channel: RealtimeChannel,
        event: TypingEvent,
    ) {
        private val typingEvent = event
        private val commands = Channel<TypingCommand>(capacity = Channel.UNLIMITED)
        private val job = viewModelScope.launch { runCoordinator() }

        fun draftChanged(isTyping: Boolean) {
            commands.trySend(
                TypingCommand.DraftChanged(
                    isTyping = isTyping,
                    changedAtMs = SystemClock.elapsedRealtime(),
                ),
            )
        }

        fun channelStatusChanged(status: RealtimeChannel.Status) {
            commands.trySend(TypingCommand.ChannelStatusChanged(status))
        }

        fun requestStop() {
            commands.trySend(TypingCommand.Stop(terminate = false, completion = null))
        }

        suspend fun stopTypingAndFlush() {
            stop(terminate = false)
        }

        suspend fun shutdown() {
            stop(terminate = true)
            job.cancelAndJoin()
        }

        private suspend fun stop(terminate: Boolean) {
            val completion = CompletableDeferred<Boolean>()
            if (!commands.trySend(TypingCommand.Stop(terminate, completion)).isSuccess) return
            val completed = withTimeoutOrNull(TYPING_ACK_TIMEOUT_MS) {
                completion.await()
            }
            if (completed != true) {
                debugLog("conversation session=$sessionSerial typing=false acknowledgement failed")
            }
        }

        private suspend fun runCoordinator() {
            var desiredTyping = false
            var subscribed = false
            var idleDeadlineMs: Long? = null
            var lastTrueSentAtMs: Long? = null

            while (currentCoroutineContext().isActive) {
                val now = SystemClock.elapsedRealtime()
                val waitMs = idleDeadlineMs?.let { (it - now).coerceAtLeast(1L) }
                val command = if (waitMs == null) {
                    commands.receive()
                } else {
                    withTimeoutOrNull(waitMs) { commands.receive() }
                }
                val commandTime = SystemClock.elapsedRealtime()

                if (desiredTyping && idleDeadlineMs?.let { commandTime >= it } == true) {
                    desiredTyping = false
                    idleDeadlineMs = null
                    if (subscribed) broadcastTyping(false)
                }

                when (command) {
                    null -> Unit
                    is TypingCommand.DraftChanged -> {
                        if (!command.isTyping || commandTime - command.changedAtMs >= TYPING_IDLE_MS) {
                            val shouldSendStop = desiredTyping
                            desiredTyping = false
                            idleDeadlineMs = null
                            if (subscribed && shouldSendStop) broadcastTyping(false)
                        } else {
                            val wasTyping = desiredTyping
                            desiredTyping = true
                            idleDeadlineMs = command.changedAtMs + TYPING_IDLE_MS
                            val shouldRefresh = lastTrueSentAtMs
                                ?.let { commandTime - it >= TYPING_REFRESH_MS }
                                ?: true
                            if (subscribed && (!wasTyping || shouldRefresh)) {
                                if (broadcastTyping(true)) lastTrueSentAtMs = commandTime
                            }
                        }
                    }
                    is TypingCommand.ChannelStatusChanged -> {
                        val wasSubscribed = subscribed
                        subscribed = command.status == RealtimeChannel.Status.SUBSCRIBED
                        if (!subscribed) {
                            lastTrueSentAtMs = null
                        } else if (!wasSubscribed && desiredTyping) {
                            if (broadcastTyping(true)) {
                                lastTrueSentAtMs = SystemClock.elapsedRealtime()
                            }
                        }
                    }
                    is TypingCommand.Stop -> {
                        desiredTyping = false
                        idleDeadlineMs = null
                        val sent = !subscribed || broadcastTyping(false)
                        command.completion?.complete(sent)
                        if (command.terminate) return
                    }
                }
            }
        }

        private suspend fun broadcastTyping(isTyping: Boolean): Boolean = try {
            withTimeout(TYPING_ACK_TIMEOUT_MS) {
                channel.broadcast(
                    TYPING_EVENT,
                    typingEvent.copy(isTyping = isTyping),
                )
            }
            true
        } catch (_: TimeoutCancellationException) {
            debugLog("conversation session=$sessionSerial typing=$isTyping acknowledgement timed out")
            false
        } catch (failure: Throwable) {
            currentCoroutineContext().ensureActive()
            debugLog(
                "conversation session=$sessionSerial typing=$isTyping acknowledgement failed " +
                    "reason=${failure::class.simpleName}",
            )
            false
        }
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(REALTIME_LOG_TAG, message)
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
        const val MESSAGE_PREFETCH_DISTANCE = 10
        const val MAX_MESSAGE_LENGTH = 2_000
        const val PENDING_MESSAGE_ID_PREFIX = "pending:"
        const val PENDING_MESSAGE_CLOCK_SKEW_MS = 5_000L
        const val PENDING_MESSAGE_CONFIRMATION_MS = 120_000L
        const val TYPING_IDLE_MS = 1_200L
        const val TYPING_REFRESH_MS = 1_000L
        const val TYPING_ACK_TIMEOUT_MS = 750L
        const val REMOTE_TYPING_TIMEOUT_MS = 4_000L
        const val MESSAGE_SYNC_FALLBACK_MS = 15_000L
        const val INBOX_SYNC_FALLBACK_MS = 5_000L
        const val RECEIPT_EVENT = "message-receipt"
        const val MESSAGE_EVENT = "message-changed"
        const val TYPING_EVENT = "typing-changed"
        const val REALTIME_LOG_TAG = "ChatRealtime"
    }
}
