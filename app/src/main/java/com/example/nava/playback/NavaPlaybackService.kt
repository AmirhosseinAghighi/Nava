package com.example.nava.playback

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
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

@androidx.annotation.OptIn(UnstableApi::class)
class NavaPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var crossfadePlayer: ExoPlayer? = null
    private lateinit var cache: SimpleCache
    private val fftAnalyzer = PlaybackFftAnalyzer(::publishFftBands)
    private val fftAudioProcessor = TeeAudioProcessor(fftAnalyzer)
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
    private val crossfadeHandler = Handler(Looper.getMainLooper())
    private var crossfadeStartedAt = 0L
    private var crossfadeRunnable: Runnable? = null
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
            else publishFftBands(FloatArray(FFT_BAND_COUNT))
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && crossfadePlayer != null) return
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
        fun createPlayer(withAnalyzer: Boolean): ExoPlayer {
            val renderersFactory = object : DefaultRenderersFactory(this) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): AudioSink = DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(if (withAnalyzer) arrayOf<AudioProcessor>(fftAudioProcessor) else emptyArray())
                    .build()
            }
            return ExoPlayer.Builder(this, renderersFactory)
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
        }
        player = createPlayer(withAnalyzer = true)
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
                cancelCrossfade()
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
            ACTION_CROSSFADE_URI -> intent.getStringExtra(EXTRA_URI)?.let { uri ->
                startCrossfade(
                    uri = uri,
                    title = intent.getStringExtra(EXTRA_TITLE),
                    artist = intent.getStringExtra(EXTRA_ARTIST),
                    artworkUri = intent.getStringExtra(EXTRA_ARTWORK_URI),
                )
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
        cancelCrossfade()
        player.removeListener(playerListener)
        session?.run { player.release(); release() }
        session = null
        cache.release()
        fftAnalyzer.release()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startCrossfade(uri: String, title: String?, artist: String?, artworkUri: String?) {
        if (crossfadePlayer != null || !player.isPlaying) return
        val incoming = createCrossfadePlayer()
        crossfadePlayer = incoming
        incoming.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (crossfadePlayer === incoming) cancelCrossfade()
            }
        })
        incoming.volume = 0f
        incoming.setMediaItem(mediaItem(uri, title, artist, artworkUri))
        incoming.prepare()
        incoming.play()
        crossfadeStartedAt = SystemClock.elapsedRealtime()
        val runnable = object : Runnable {
            override fun run() {
                val progress = ((SystemClock.elapsedRealtime() - crossfadeStartedAt).toFloat() / CROSSFADE_DURATION_MS).coerceIn(0f, 1f)
                player.volume = 1f - progress
                incoming.volume = progress
                if (progress < 1f) crossfadeHandler.postDelayed(this, CROSSFADE_TICK_MS)
                else finishCrossfade(incoming)
            }
        }
        crossfadeRunnable = runnable
        crossfadeHandler.post(runnable)
    }

    private fun createCrossfadePlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(this)
        return ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(DefaultDataSource.Factory(this)),
            ))
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false,
                )
            }
    }

    private fun finishCrossfade(incoming: ExoPlayer) {
        if (crossfadePlayer !== incoming) return
        crossfadeHandler.removeCallbacks(crossfadeRunnable ?: return)
        crossfadeRunnable = null
        player.removeListener(playerListener)
        player.stop()
        player.release()
        player = incoming
        player.volume = 1f
        player.addListener(playerListener)
        crossfadePlayer = null
        session?.setPlayer(player)
        publishPlaybackState()
        sendBroadcast(Intent(ACTION_CROSSFADE_COMPLETE).setPackage(packageName))
    }

    private fun cancelCrossfade() {
        crossfadeRunnable?.let(crossfadeHandler::removeCallbacks)
        crossfadeRunnable = null
        crossfadePlayer?.run {
            stop()
            release()
        }
        crossfadePlayer = null
        if (::player.isInitialized) player.volume = 1f
    }

    private fun mediaItem(uri: String, title: String?, artist: String?, artworkUri: String?): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri?.let(android.net.Uri::parse))
                    .build(),
            )
            .build()

    @SuppressLint("MissingPermission")
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
        if (!canPostNotifications()) return
        getSystemService(NotificationManager::class.java)
            .notify(PLAYBACK_NOTIFICATION_ID, buildPlaybackNotification())
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
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

    private fun publishFftBands(bands: FloatArray) {
        sendBroadcast(
            Intent(ACTION_FFT_DATA)
                .setPackage(packageName)
                .putExtra(EXTRA_FFT_BANDS, bands),
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
        const val ACTION_FFT_DATA = "com.example.nava.playback.FFT_DATA"
        const val ACTION_SKIP_NEXT = "com.example.nava.playback.SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.example.nava.playback.SKIP_PREVIOUS"
        const val ACTION_CROSSFADE_COMPLETE = "com.example.nava.playback.CROSSFADE_COMPLETE"
        const val ACTION_CROSSFADE_URI = "com.example.nava.playback.CROSSFADE_URI"
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
        const val EXTRA_FFT_BANDS = "fft_bands"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_SLEEP_MS = "sleep_ms"
        private const val STATE_TICK_INTERVAL_MS = 1_000L
        private const val CROSSFADE_DURATION_MS = 5_000L
        private const val CROSSFADE_TICK_MS = 50L
        private const val PLAYBACK_CHANNEL_ID = "nava_playback"
        private const val PLAYBACK_NOTIFICATION_ID = 1001
        private const val FFT_BAND_COUNT = 28
    }
}
