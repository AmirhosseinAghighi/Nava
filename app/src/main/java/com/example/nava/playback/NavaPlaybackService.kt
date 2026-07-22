package com.example.nava.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.nava.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.File

class NavaPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var cache: SimpleCache
    private var session: MediaSession? = null
    private var currentTitle: String? = null
    private var currentArtist: String? = null
    private val previousSessionCommand = SessionCommand(SESSION_COMMAND_PREVIOUS, Bundle.EMPTY)
    private val nextSessionCommand = SessionCommand(SESSION_COMMAND_NEXT, Bundle.EMPTY)
    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(previousSessionCommand)
                .add(nextSessionCommand)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                SESSION_COMMAND_PREVIOUS -> sendSkipBroadcast(ACTION_SKIP_PREVIOUS)
                SESSION_COMMAND_NEXT -> sendSkipBroadcast(ACTION_SKIP_NEXT)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
    private val sleepHandler = Handler(Looper.getMainLooper())
    private val stateHandler = Handler(Looper.getMainLooper())
    private val sleepRunnable = Runnable { player.pause() }
    private val stateTicker = object : Runnable {
        override fun run() {
            publishPlaybackState()
            if (player.isPlaying) stateHandler.postDelayed(this, STATE_TICK_INTERVAL_MS)
        }
    }
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishPlaybackState()
            updatePlaybackNotification()
            stateHandler.removeCallbacks(stateTicker)
            if (isPlaying) stateHandler.post(stateTicker)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            publishPlaybackState()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            publishPlaybackState(hasError = true)
        }
    }
    override fun onCreate() {
        super.onCreate()
        cache = SimpleCache(
            File(cacheDir, "nava_audio_cache"),
            LeastRecentlyUsedCacheEvictor(128L * 1024L * 1024L),
            StandaloneDatabaseProvider(this),
        )
        val sourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(this))
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(sourceFactory))
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
            }
        player.addListener(playerListener)
        val previousButton = CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setDisplayName(getString(R.string.previous_track))
            .setSessionCommand(previousSessionCommand)
            .build()
        val nextButton = CommandButton.Builder(CommandButton.ICON_NEXT)
            .setDisplayName(getString(R.string.next_track))
            .setSessionCommand(nextSessionCommand)
            .build()
        session = MediaSession.Builder(this, player)
            .setCallback(mediaSessionCallback)
            .setMediaButtonPreferences(ImmutableList.of(previousButton, nextButton))
            .setCustomLayout(ImmutableList.of(previousButton, nextButton))
            .build()
    }
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        val serviceResult = super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY_URI -> intent.getStringExtra(EXTRA_URI)?.let { uri ->
                startPlaybackForeground(intent.getStringExtra(EXTRA_TITLE))
                currentTitle = intent.getStringExtra(EXTRA_TITLE)
                currentArtist = intent.getStringExtra(EXTRA_ARTIST)
                player.setMediaItem(
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(intent.getStringExtra(EXTRA_TITLE))
                                .setArtist(intent.getStringExtra(EXTRA_ARTIST))
                                .setArtworkUri(intent.getStringExtra(EXTRA_ARTWORK_URI)?.let(android.net.Uri::parse))
                                .build(),
                        )
                        .build(),
                )
                player.prepare()
                player.play()
            }
            ACTION_PAUSE -> player.pause()
            ACTION_RESUME -> player.play()
            ACTION_SEEK -> player.seekTo(intent.getLongExtra(EXTRA_POSITION_MS, player.currentPosition))
            ACTION_SPEED -> player.setPlaybackSpeed(intent.getFloatExtra(EXTRA_SPEED, 1f))
            ACTION_SLEEP -> {
                sleepHandler.removeCallbacks(sleepRunnable)
                intent.getLongExtra(EXTRA_SLEEP_MS, 0L)
                    .takeIf { it > 0L }
                    ?.let { delayMs -> sleepHandler.postDelayed(sleepRunnable, delayMs) }
            }
            ACTION_NEXT -> sendSkipBroadcast(ACTION_SKIP_NEXT)
            ACTION_PREVIOUS -> sendSkipBroadcast(ACTION_SKIP_PREVIOUS)
        }
        return serviceResult
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() {
        sleepHandler.removeCallbacksAndMessages(null)
        stateHandler.removeCallbacksAndMessages(null)
        player.removeListener(playerListener)
        session?.run { player.release(); release() }
        session = null
        cache.release()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startPlaybackForeground(title: String?) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        currentTitle = title
        val notification = buildPlaybackNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                PLAYBACK_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(PLAYBACK_NOTIFICATION_ID, notification)
        }
    }

    private fun updatePlaybackNotification() {
        if (!::player.isInitialized) return
        getSystemService(NotificationManager::class.java)
            .notify(PLAYBACK_NOTIFICATION_ID, buildPlaybackNotification())
    }

    private fun buildPlaybackNotification() = NotificationCompat.Builder(this, PLAYBACK_CHANNEL_ID)
        .setSmallIcon(if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        .setContentTitle(currentTitle ?: getString(R.string.app_name))
        .setContentText(currentArtist)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(android.R.drawable.ic_media_previous, getString(R.string.previous_track), controlIntent(ACTION_PREVIOUS, 1))
        .addAction(
            if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            getString(if (player.isPlaying) R.string.pause_playback else R.string.resume_playback),
            controlIntent(if (player.isPlaying) ACTION_PAUSE else ACTION_RESUME, 2),
        )
        .addAction(android.R.drawable.ic_media_next, getString(R.string.next_track), controlIntent(ACTION_NEXT, 3))
        .apply {
            session?.let {
                setStyle(
                    MediaStyleNotificationHelper.MediaStyle(it)
                        .setShowActionsInCompactView(0, 1, 2),
                )
            }
        }
        .build()

    private fun controlIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, NavaPlaybackService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun sendSkipBroadcast(action: String) {
        sendBroadcast(Intent(action).setPackage(packageName))
    }

    private fun publishPlaybackState(hasError: Boolean = false) {
        sendBroadcast(
            android.content.Intent(ACTION_PLAYBACK_STATE)
                .setPackage(packageName)
                .putExtra(EXTRA_PLAYING, player.isPlaying)
                .putExtra(EXTRA_POSITION_MS, player.currentPosition.coerceAtLeast(0L))
                .putExtra(EXTRA_DURATION_MS, player.duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L)
                .putExtra(EXTRA_PLAYBACK_STATE, player.playbackState)
                .putExtra(EXTRA_ERROR, hasError),
        )
    }

    companion object {
        const val ACTION_PLAY_URI = "com.example.nava.playback.PLAY_URI"
        const val ACTION_PAUSE = "com.example.nava.playback.PAUSE"
        const val ACTION_RESUME = "com.example.nava.playback.RESUME"
        const val ACTION_SEEK = "com.example.nava.playback.SEEK"
        const val ACTION_SPEED = "com.example.nava.playback.SPEED"
        const val ACTION_SLEEP = "com.example.nava.playback.SLEEP"
        const val ACTION_NEXT = "com.example.nava.playback.NEXT"
        const val ACTION_PREVIOUS = "com.example.nava.playback.PREVIOUS"
        const val ACTION_PLAYBACK_STATE = "com.example.nava.playback.STATE_CHANGED"
        const val ACTION_SKIP_NEXT = "com.example.nava.playback.SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.example.nava.playback.SKIP_PREVIOUS"
        const val SESSION_COMMAND_NEXT = "com.example.nava.playback.SESSION_NEXT"
        const val SESSION_COMMAND_PREVIOUS = "com.example.nava.playback.SESSION_PREVIOUS"
        const val EXTRA_URI = "uri"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ARTWORK_URI = "artwork_uri"
        const val EXTRA_POSITION_MS = "position_ms"
        const val EXTRA_DURATION_MS = "duration_ms"
        const val EXTRA_PLAYING = "playing"
        const val EXTRA_PLAYBACK_STATE = "playback_state"
        const val EXTRA_ERROR = "error"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_SLEEP_MS = "sleep_ms"
        private const val STATE_TICK_INTERVAL_MS = 1_000L
        private const val PLAYBACK_CHANNEL_ID = "nava_playback"
        private const val PLAYBACK_NOTIFICATION_ID = 1001
    }
}
