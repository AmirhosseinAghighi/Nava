package com.example.nava.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import android.os.Handler
import android.os.Looper

class NavaPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val sleepHandler = Handler(Looper.getMainLooper())
    private val sleepRunnable = Runnable { player.pause() }
    override fun onCreate() { super.onCreate(); player = ExoPlayer.Builder(this).build().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true) }; session = MediaSession.Builder(this, player).build() }
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_URI -> intent.getStringExtra(EXTRA_URI)?.let { uri -> player.setMediaItem(MediaItem.fromUri(uri)); player.prepare(); player.play() }
            ACTION_PAUSE -> player.pause()
            ACTION_SEEK -> player.seekTo(intent.getLongExtra(EXTRA_POSITION_MS, player.currentPosition))
            ACTION_SPEED -> player.setPlaybackSpeed(intent.getFloatExtra(EXTRA_SPEED, 1f))
            ACTION_SLEEP -> { sleepHandler.removeCallbacks(sleepRunnable); sleepHandler.postDelayed(sleepRunnable, intent.getLongExtra(EXTRA_SLEEP_MS, 0)) }
        }
        return START_STICKY
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() { sleepHandler.removeCallbacksAndMessages(null); session?.run { player.release(); release() }; session = null; super.onDestroy() }
    companion object { const val ACTION_PLAY_URI = "com.example.nava.playback.PLAY_URI"; const val ACTION_PAUSE = "com.example.nava.playback.PAUSE"; const val ACTION_SEEK = "com.example.nava.playback.SEEK"; const val ACTION_SPEED = "com.example.nava.playback.SPEED"; const val ACTION_SLEEP = "com.example.nava.playback.SLEEP"; const val EXTRA_URI = "uri"; const val EXTRA_POSITION_MS = "position_ms"; const val EXTRA_SPEED = "speed"; const val EXTRA_SLEEP_MS = "sleep_ms" }
}
