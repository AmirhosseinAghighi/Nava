package com.example.nava.playback

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.domain.catalog.HomeTrack
import com.example.nava.data.downloads.OfflineDownloadRepository
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private var shuffleSource: List<HomeTrack> = emptyList()
    private val playbackHistory = mutableListOf<HomeTrack>()
    private var isAdvancing = false
    private var lastRecordedProgressMs = 0L
    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NavaPlaybackService.ACTION_SKIP_NEXT -> skipToNext()
                NavaPlaybackService.ACTION_SKIP_PREVIOUS -> skipToPrevious()
                NavaPlaybackService.ACTION_PLAYBACK_STATE -> updatePlaybackState(intent)
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
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun play(track: HomeTrack) = viewModelScope.launch {
        rememberCurrentTrack()
        playTrack(track)
    }

    fun addToQueue(track: HomeTrack) {
        _userQueue.value = _userQueue.value + track
    }

    fun setShuffleSource(tracks: List<HomeTrack>) {
        shuffleSource = tracks.distinctBy(HomeTrack::id)
    }

    private suspend fun playTrack(track: HomeTrack): Boolean {
        runCatching {
            offlineDownloads.find(track.id)
                ?.audioPath
                ?.let(::File)
                ?.takeIf(File::exists)
                ?.toURI()
                ?.toString()
                ?: resolver.resolve(track.audioUrl)
        }
            .onSuccess { url ->
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
    fun setSpeed(speed: Float) = send(NavaPlaybackService.ACTION_SPEED, NavaPlaybackService.EXTRA_SPEED to speed)
    fun setSleepTimer(minutes: Long) = send(NavaPlaybackService.ACTION_SLEEP, NavaPlaybackService.EXTRA_SLEEP_MS to minutes * 60_000L)
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
        advanceToNext(current)
    }

    private fun advanceToNext(current: NowPlaying) {
        if (isAdvancing) return
        val queuedTrack = _userQueue.value.firstOrNull()
        val nextTrack = queuedTrack ?: shuffleSource
            .filterNot { it.id == current.track.id }
            .shuffled()
            .firstOrNull()
            ?: return
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

    private fun send(action: String, extra: Pair<String, Any>) {
        Intent(getApplication(), NavaPlaybackService::class.java).setAction(action).also { intent ->
            when (val value = extra.second) { is Long -> intent.putExtra(extra.first, value); is Float -> intent.putExtra(extra.first, value) }
            getApplication<Application>().startService(intent)
        }
    }

    private companion object {
        const val PROGRESS_REPORT_INTERVAL_MS = 30_000L
        const val RESTART_POSITION_MS = 5_000L
    }

    private fun rememberCurrentTrack() {
        _nowPlaying.value?.track?.let { currentTrack ->
            if (playbackHistory.lastOrNull()?.id != currentTrack.id) {
                playbackHistory += currentTrack
            }
        }
    }
}
