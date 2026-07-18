package com.example.nava.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class NavaPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    override fun onCreate() { super.onCreate(); player = ExoPlayer.Builder(this).build().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true) }; session = MediaSession.Builder(this, player).build() }
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_URI -> intent.getStringExtra(EXTRA_URI)?.let { uri -> player.setMediaItem(MediaItem.fromUri(uri)); player.prepare(); player.play() }
            ACTION_PAUSE -> player.pause()
        }
        return START_STICKY
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() { session?.run { player.release(); release() }; session = null; super.onDestroy() }
    companion object { const val ACTION_PLAY_URI = "com.example.nava.playback.PLAY_URI"; const val ACTION_PAUSE = "com.example.nava.playback.PAUSE"; const val EXTRA_URI = "uri" }
}
