package com.example.nava.playback

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.domain.catalog.HomeTrack
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

data class NowPlaying(val track: HomeTrack, val playing: Boolean)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    application: Application,
    private val resolver: SignedAudioUrlResolver,
    private val supabase: SupabaseClient,
) : AndroidViewModel(application) {
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    fun play(track: HomeTrack) = viewModelScope.launch {
        runCatching { resolver.resolve(track.audioUrl) }.onSuccess { url ->
            getApplication<Application>().startForegroundService(Intent(getApplication(), NavaPlaybackService::class.java).apply {
                action = NavaPlaybackService.ACTION_PLAY_URI
                putExtra(NavaPlaybackService.EXTRA_URI, url)
            })
            _nowPlaying.value = NowPlaying(track, true)
            runCatching { supabase.postgrest.rpc("record_playback_event", buildJsonObject { put("p_track_id", track.id); put("p_event_type", "started") }) }
        }
    }

    fun pause() {
        getApplication<Application>().startService(Intent(getApplication(), NavaPlaybackService::class.java).setAction(NavaPlaybackService.ACTION_PAUSE))
        _nowPlaying.value = _nowPlaying.value?.copy(playing = false)
    }

    fun seekTo(positionMs: Long) = send(NavaPlaybackService.ACTION_SEEK, NavaPlaybackService.EXTRA_POSITION_MS to positionMs)
    fun setSpeed(speed: Float) = send(NavaPlaybackService.ACTION_SPEED, NavaPlaybackService.EXTRA_SPEED to speed)
    fun setSleepTimer(minutes: Long) = send(NavaPlaybackService.ACTION_SLEEP, NavaPlaybackService.EXTRA_SLEEP_MS to minutes * 60_000L)
    private fun send(action: String, extra: Pair<String, Any>) {
        Intent(getApplication(), NavaPlaybackService::class.java).setAction(action).also { intent ->
            when (val value = extra.second) { is Long -> intent.putExtra(extra.first, value); is Float -> intent.putExtra(extra.first, value) }
            getApplication<Application>().startService(intent)
        }
    }
}
