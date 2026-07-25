package com.example.nava.playback

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.domain.catalog.HomeTrack
import com.example.nava.data.downloads.OfflineDownloadRepository
import com.example.nava.data.downloads.OfflineTrackEntity
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

data class NowPlaying(
    val track: HomeTrack,
    val playing: Boolean,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

enum class RepeatMode { Off, All, One }

fun OfflineTrackEntity.toHomeTrack() = HomeTrack(
    id = trackId,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioPath,
    languageCode = "",
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    application: Application,
    private val resolver: SignedAudioUrlResolver,
    private val offlineDownloads: OfflineDownloadRepository,
    private val supabase: SupabaseClient,
) : AndroidViewModel(application) {
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()
    private val _playbackError = MutableStateFlow(false)
    val playbackError: StateFlow<Boolean> = _playbackError.asStateFlow()
    private val _userQueue = MutableStateFlow<List<HomeTrack>>(emptyList())
    val userQueue: StateFlow<List<HomeTrack>> = _userQueue.asStateFlow()
    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    private val _sleepTimerMinutes = MutableStateFlow<Long?>(null)
    val sleepTimerMinutes: StateFlow<Long?> = _sleepTimerMinutes.asStateFlow()
    private val _fftBands = MutableStateFlow(FloatArray(FFT_BAND_COUNT))
    val fftBands: StateFlow<FloatArray> = _fftBands.asStateFlow()
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()
    private val _repeatMode = MutableStateFlow(RepeatMode.Off)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    /** The ordered collection the current track was started from — a playlist, a feed, a search page. */
    private var playbackSource: List<HomeTrack> = emptyList()

    /**
     * A full permutation of [playbackSource] used while shuffle is on, so a shuffled run visits
     * every track once instead of picking a fresh random track each time.
     */
    private var shuffleOrder: List<HomeTrack> = emptyList()
    private val playbackHistory = mutableListOf<HomeTrack>()
    private var sleepTimerResetJob: Job? = null
    private var isAdvancing = false
    private var crossfadeRequestedFor: String? = null
    private var crossfadeTrack: HomeTrack? = null
    private var prefetchJob: Job? = null
    private var prefetchedTrackId: String? = null
    private var prefetchedUrl: String? = null
    private var lastRecordedProgressMs = 0L
    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NavaPlaybackService.ACTION_SKIP_NEXT -> skipToNext()
                NavaPlaybackService.ACTION_SKIP_PREVIOUS -> skipToPrevious()
                NavaPlaybackService.ACTION_PLAYBACK_STATE -> updatePlaybackState(intent)
                NavaPlaybackService.ACTION_CROSSFADE_COMPLETE -> completeCrossfade()
                NavaPlaybackService.ACTION_CROSSFADE_ABORTED -> abandonCrossfade()
                NavaPlaybackService.ACTION_FFT_DATA -> intent
                    .getFloatArrayExtra(NavaPlaybackService.EXTRA_FFT_BANDS)
                    ?.takeIf { it.size == FFT_BAND_COUNT }
                    ?.let { _fftBands.value = it }
            }
        }

        private fun updatePlaybackState(intent: Intent) {
            if (intent.getBooleanExtra(NavaPlaybackService.EXTRA_ERROR, false)) _playbackError.value = true
            val current = _nowPlaying.value ?: return
            val positionMs = intent.getLongExtra(NavaPlaybackService.EXTRA_POSITION_MS, current.positionMs)
            val playbackState = intent.getIntExtra(NavaPlaybackService.EXTRA_PLAYBACK_STATE, 0)
            _nowPlaying.value = current.copy(
                playing = intent.getBooleanExtra(NavaPlaybackService.EXTRA_PLAYING, current.playing),
                positionMs = positionMs,
                durationMs = intent.getLongExtra(NavaPlaybackService.EXTRA_DURATION_MS, current.durationMs),
            )
            val durationMs = _nowPlaying.value?.durationMs ?: 0L
            maybePrefetchNext(current, positionMs, durationMs)
            maybeStartCrossfade(current, positionMs, durationMs)
            when {
                playbackState == androidx.media3.common.Player.STATE_ENDED -> {
                    recordEvent(current.track.id, "completed", positionMs)
                    advanceAfterCompletion(current)
                }
                positionMs - lastRecordedProgressMs >= PROGRESS_REPORT_INTERVAL_MS -> {
                    lastRecordedProgressMs = positionMs
                    recordEvent(current.track.id, "progress", positionMs)
                }
            }
        }
    }

    init {
        ContextCompat.registerReceiver(
            getApplication(),
            playbackStateReceiver,
            IntentFilter().apply {
                addAction(NavaPlaybackService.ACTION_PLAYBACK_STATE)
                addAction(NavaPlaybackService.ACTION_SKIP_NEXT)
                addAction(NavaPlaybackService.ACTION_SKIP_PREVIOUS)
                addAction(NavaPlaybackService.ACTION_FFT_DATA)
                addAction(NavaPlaybackService.ACTION_CROSSFADE_COMPLETE)
                addAction(NavaPlaybackService.ACTION_CROSSFADE_ABORTED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /**
     * Plays [track]. Pass [context] — the ordered collection the tap came from — so playback keeps
     * following that order afterwards instead of drifting into whatever was queued up last.
     */
    fun play(track: HomeTrack, context: List<HomeTrack>? = null) = viewModelScope.launch {
        context?.let(::setPlaybackSource)
        rememberCurrentTrack()
        playTrack(track)
    }

    /** Starts an ordered collection from its first track, or from a random one when [shuffle] is set. */
    fun playCollection(tracks: List<HomeTrack>, shuffle: Boolean = false) {
        val ordered = tracks.distinctBy(HomeTrack::id)
        if (ordered.isEmpty()) return
        setPlaybackSource(ordered)
        _shuffleEnabled.value = shuffle
        shuffleOrder = emptyList()
        _userQueue.value = emptyList()
        val first = if (shuffle) ordered.random() else ordered.first()
        viewModelScope.launch {
            rememberCurrentTrack()
            playTrack(first)
        }
    }

    fun playOffline(download: OfflineTrackEntity, context: List<HomeTrack>? = null) = viewModelScope.launch {
        context?.let(::setPlaybackSource)
        rememberCurrentTrack()
        playTrack(download.toHomeTrack(), download.audioPath)
    }

    fun addToQueue(track: HomeTrack) {
        _userQueue.value = _userQueue.value + track
        // The track that comes next just changed, so any pending prefetch is stale.
        clearPrefetch()
    }

    fun setShuffleSource(tracks: List<HomeTrack>) {
        setPlaybackSource(tracks)
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
        shuffleOrder = emptyList()
        clearPrefetch()
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        clearPrefetch()
    }

    private fun setPlaybackSource(tracks: List<HomeTrack>) {
        val deduped = tracks.distinctBy(HomeTrack::id)
        if (deduped.map(HomeTrack::id) == playbackSource.map(HomeTrack::id)) return
        playbackSource = deduped
        shuffleOrder = emptyList()
        clearPrefetch()
    }

    private suspend fun playTrack(track: HomeTrack, localAudioPath: String? = null): Boolean {
        runCatching {
            resolveAudioUrl(track, localAudioPath)
        }
            .onSuccess { url ->
                crossfadeRequestedFor = null
                crossfadeTrack = null
                clearPrefetch()
                _playbackError.value = false
                getApplication<Application>().startForegroundService(Intent(getApplication(), NavaPlaybackService::class.java).apply {
                    action = NavaPlaybackService.ACTION_PLAY_URI
                    putExtra(NavaPlaybackService.EXTRA_URI, url)
                    putExtra(NavaPlaybackService.EXTRA_TITLE, track.title)
                    putExtra(NavaPlaybackService.EXTRA_ARTIST, track.artistName)
                    putExtra(NavaPlaybackService.EXTRA_ARTWORK_URI, track.coverImageUrl)
                })
                _nowPlaying.value = NowPlaying(track, true)
                lastRecordedProgressMs = 0L
                recordEvent(track.id, "started", 0L)
                return true
            }
            .onFailure { _playbackError.value = true }
        return false
    }

    private suspend fun resolveAudioUrl(track: HomeTrack, localAudioPath: String? = null): String =
        localAudioPath
            ?.let(::File)
            ?.takeIf(File::exists)
            ?.let(Uri::fromFile)
            ?.toString()
            ?: offlineDownloads.find(track.id)
                ?.audioPath
                ?.let(::File)
                ?.takeIf(File::exists)
                ?.let(Uri::fromFile)
                ?.toString()
            ?: resolver.resolve(track.audioUrl)

    /**
     * Resolves the upcoming track's URL well before the fade is due. Resolving a signed URL is a
     * network round trip; doing it at the fade boundary made the fade start late (or miss entirely)
     * and the track then had to be fetched a second time when it was actually played.
     */
    private fun maybePrefetchNext(current: NowPlaying, positionMs: Long, durationMs: Long) {
        if (_repeatMode.value == RepeatMode.One || crossfadeTrack != null) return
        if (durationMs <= 0L || durationMs - positionMs > PREFETCH_LEAD_MS) return
        val nextTrack = nextTrackAfter(current.track) ?: return
        if (prefetchedTrackId == nextTrack.id || prefetchJob?.isActive == true) return
        prefetchedTrackId = nextTrack.id
        prefetchedUrl = null
        prefetchJob = viewModelScope.launch {
            val url = runCatching { resolveAudioUrl(nextTrack) }.getOrNull()
            if (prefetchedTrackId == nextTrack.id) prefetchedUrl = url
        }
    }

    private fun maybeStartCrossfade(current: NowPlaying, positionMs: Long, durationMs: Long) {
        if (
            _repeatMode.value == RepeatMode.One ||
            durationMs <= CROSSFADE_DURATION_MS ||
            durationMs - positionMs > CROSSFADE_DURATION_MS ||
            crossfadeRequestedFor == current.track.id ||
            crossfadeTrack != null
        ) return
        val nextTrack = nextTrackAfter(current.track) ?: return
        crossfadeRequestedFor = current.track.id
        prefetchedUrl?.takeIf { prefetchedTrackId == nextTrack.id }?.let { url ->
            crossfadeTrack = nextTrack
            requestCrossfade(url, nextTrack)
            return
        }
        viewModelScope.launch {
            val url = runCatching { resolveAudioUrl(nextTrack) }.getOrNull()
            if (url == null || _nowPlaying.value?.track?.id != current.track.id) {
                if (_nowPlaying.value?.track?.id == current.track.id) crossfadeRequestedFor = null
                return@launch
            }
            crossfadeTrack = nextTrack
            requestCrossfade(url, nextTrack)
        }
    }

    private fun requestCrossfade(url: String, nextTrack: HomeTrack) {
        getApplication<Application>().startForegroundService(Intent(getApplication(), NavaPlaybackService::class.java).apply {
            action = NavaPlaybackService.ACTION_CROSSFADE_URI
            putExtra(NavaPlaybackService.EXTRA_URI, url)
            putExtra(NavaPlaybackService.EXTRA_TITLE, nextTrack.title)
            putExtra(NavaPlaybackService.EXTRA_ARTIST, nextTrack.artistName)
            putExtra(NavaPlaybackService.EXTRA_ARTWORK_URI, nextTrack.coverImageUrl)
        })
    }

    /**
     * The service already promoted the faded-in player, so the track is mid-playback: adopt it as
     * now-playing rather than starting it over.
     */
    private fun completeCrossfade() {
        val nextTrack = crossfadeTrack ?: return
        rememberCurrentTrack()
        _nowPlaying.value = NowPlaying(nextTrack, true)
        crossfadeTrack = null
        crossfadeRequestedFor = null
        clearPrefetch()
        lastRecordedProgressMs = 0L
        recordEvent(nextTrack.id, "started", 0L)
        if (_userQueue.value.firstOrNull()?.id == nextTrack.id) {
            _userQueue.value = _userQueue.value.drop(1)
        }
    }

    /**
     * The fade was dropped (error, seek, or the incoming track never started). Release the
     * bookkeeping so the normal end-of-track advance can take over.
     */
    private fun abandonCrossfade() {
        crossfadeTrack = null
        crossfadeRequestedFor = null
    }

    private fun clearPrefetch() {
        prefetchJob?.cancel()
        prefetchJob = null
        prefetchedTrackId = null
        prefetchedUrl = null
    }

    fun pause() {
        getApplication<Application>().startService(Intent(getApplication(), NavaPlaybackService::class.java).setAction(NavaPlaybackService.ACTION_PAUSE))
        _nowPlaying.value = _nowPlaying.value?.copy(playing = false)
    }

    fun resume() {
        getApplication<Application>().startService(Intent(getApplication(), NavaPlaybackService::class.java).setAction(NavaPlaybackService.ACTION_RESUME))
        _nowPlaying.value = _nowPlaying.value?.copy(playing = true)
    }

    fun skipToNext() {
        _nowPlaying.value?.let(::advanceToNext)
    }

    fun skipToPrevious() {
        val current = _nowPlaying.value ?: return
        if (current.positionMs > RESTART_POSITION_MS) {
            seekTo(0L)
            return
        }
        val previousTrack = playbackHistory.removeLastOrNull() ?: return
        viewModelScope.launch { playTrack(previousTrack) }
    }

    fun seekTo(positionMs: Long) = send(NavaPlaybackService.ACTION_SEEK, NavaPlaybackService.EXTRA_POSITION_MS to positionMs)
    fun cycleSpeed() {
        val currentIndex = SPEED_OPTIONS.indexOf(_playbackSpeed.value).takeIf { it >= 0 } ?: 0
        setSpeed(SPEED_OPTIONS[(currentIndex + 1) % SPEED_OPTIONS.size])
    }

    fun cycleSleepTimer() {
        val currentIndex = SLEEP_TIMER_OPTIONS.indexOf(_sleepTimerMinutes.value).takeIf { it >= 0 } ?: 0
        setSleepTimer(SLEEP_TIMER_OPTIONS[(currentIndex + 1) % SLEEP_TIMER_OPTIONS.size])
    }

    private fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        send(NavaPlaybackService.ACTION_SPEED, NavaPlaybackService.EXTRA_SPEED to speed)
    }

    private fun setSleepTimer(minutes: Long?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerResetJob?.cancel()
        send(NavaPlaybackService.ACTION_SLEEP, NavaPlaybackService.EXTRA_SLEEP_MS to (minutes?.times(60_000L) ?: 0L))
        if (minutes != null) {
            sleepTimerResetJob = viewModelScope.launch {
                delay(minutes * 60_000L)
                if (_sleepTimerMinutes.value == minutes) _sleepTimerMinutes.value = null
            }
        }
    }
    fun clearPlaybackError() { _playbackError.value = false }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(playbackStateReceiver)
        super.onCleared()
    }

    private fun recordEvent(trackId: String, eventType: String, positionMs: Long) = viewModelScope.launch {
        runCatching {
            supabase.postgrest.rpc(
                "record_playback_event",
                buildJsonObject {
                    put("p_track_id", trackId)
                    put("p_event_type", eventType)
                    put("p_position_seconds", (positionMs / 1_000L).toInt())
                },
            )
        }
    }

    private fun advanceAfterCompletion(current: NowPlaying) {
        if (_repeatMode.value == RepeatMode.One) restartCurrent(current)
        else advanceToNext(current)
    }

    private fun advanceToNext(current: NowPlaying) {
        if (isAdvancing) return
        val queuedTrack = _userQueue.value.firstOrNull()
        val nextTrack = queuedTrack ?: sourceTrackAfter(current.track)
        if (nextTrack == null) {
            if (_repeatMode.value == RepeatMode.All) restartCurrent(current)
            return
        }
        rememberCurrentTrack()
        isAdvancing = true
        viewModelScope.launch {
            try {
                if (playTrack(nextTrack) && queuedTrack != null) {
                    _userQueue.value = _userQueue.value.drop(1)
                }
            } finally {
                isAdvancing = false
            }
        }
    }

    private fun nextTrackAfter(current: HomeTrack): HomeTrack? =
        _userQueue.value.firstOrNull() ?: sourceTrackAfter(current)

    private fun sourceTrackAfter(current: HomeTrack): HomeTrack? {
        if (playbackSource.isEmpty()) return null
        val order = if (_shuffleEnabled.value) shuffleOrderFrom(current) else playbackSource
        val currentIndex = order.indexOfFirst { it.id == current.id }
        if (currentIndex < 0) return order.firstOrNull { it.id != current.id }
        order.getOrNull(currentIndex + 1)?.let { return it }
        if (_repeatMode.value != RepeatMode.All) return null
        // Wrapping round: reshuffle so the next pass is a different order.
        if (!_shuffleEnabled.value) return order.firstOrNull()
        shuffleOrder = emptyList()
        return shuffleOrderFrom(current).firstOrNull { it.id != current.id } ?: order.firstOrNull()
    }

    /**
     * A permutation of the source that starts at [current], so walking it forward covers the whole
     * collection exactly once.
     */
    private fun shuffleOrderFrom(current: HomeTrack): List<HomeTrack> {
        val sourceIds = playbackSource.mapTo(mutableSetOf(), HomeTrack::id)
        val stale = shuffleOrder.size != playbackSource.size ||
            shuffleOrder.any { it.id !in sourceIds } ||
            shuffleOrder.firstOrNull()?.id != current.id
        if (stale) {
            val head = playbackSource.filter { it.id == current.id }
            val tail = playbackSource.filterNot { it.id == current.id }.shuffled()
            shuffleOrder = head + tail
        }
        return shuffleOrder
    }

    private fun restartCurrent(current: NowPlaying) {
        if (isAdvancing) return
        isAdvancing = true
        seekTo(0L)
        send(NavaPlaybackService.ACTION_RESUME)
        _nowPlaying.value = current.copy(playing = true, positionMs = 0L)
        lastRecordedProgressMs = 0L
        recordEvent(current.track.id, "started", 0L)
        viewModelScope.launch {
            delay(REPEAT_GUARD_INTERVAL_MS)
            isAdvancing = false
        }
    }

    private fun send(action: String, extra: Pair<String, Any>) {
        Intent(getApplication(), NavaPlaybackService::class.java).setAction(action).also { intent ->
            when (val value = extra.second) { is Long -> intent.putExtra(extra.first, value); is Float -> intent.putExtra(extra.first, value) }
            getApplication<Application>().startService(intent)
        }
    }

    private fun send(action: String) {
        getApplication<Application>().startService(
            Intent(getApplication(), NavaPlaybackService::class.java).setAction(action),
        )
    }

    private companion object {
        const val PROGRESS_REPORT_INTERVAL_MS = 30_000L
        const val RESTART_POSITION_MS = 5_000L
        const val CROSSFADE_DURATION_MS = NavaPlaybackService.CROSSFADE_DURATION_MS
        /** How long before the end of a track the next one's URL is resolved. */
        const val PREFETCH_LEAD_MS = 20_000L
        const val REPEAT_GUARD_INTERVAL_MS = 300L
        const val FFT_BAND_COUNT = 28
        val SPEED_OPTIONS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
        val SLEEP_TIMER_OPTIONS = listOf<Long?>(null, 15L, 30L, 45L, 60L)
    }

    private fun rememberCurrentTrack() {
        _nowPlaying.value?.track?.let { currentTrack ->
            if (playbackHistory.lastOrNull()?.id != currentTrack.id) {
                playbackHistory += currentTrack
            }
        }
    }
}
