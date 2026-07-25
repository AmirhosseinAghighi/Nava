package com.example.nava.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import java.io.ByteArrayOutputStream
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
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
import androidx.media3.session.MediaNotification
import com.example.nava.MainActivity
import com.example.nava.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

@androidx.annotation.OptIn(UnstableApi::class)
class NavaPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var forwardingPlayer: ForwardingPlayer
    private lateinit var cache: SimpleCache
    private lateinit var sourceFactory: CacheDataSource.Factory
    // Read from the analyzer's worker thread, written on the main thread when players swap.
    @Volatile private var activeAnalyzer: PlaybackFftAnalyzer? = null
    private var crossfadePlayer: ExoPlayer? = null
    private var crossfadeAnalyzer: PlaybackFftAnalyzer? = null
    private var session: MediaSession? = null
    private var currentTitle: String? = null
    private var currentArtist: String? = null
    private var currentArtworkBitmap: Bitmap? = null
    private var isUpdatingMetadata = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * True from the moment an incoming track is handed to the service until the fade completes or
     * is abandoned. While this holds, the outgoing player's `STATE_ENDED` must never be published:
     * the queue owner would read it as "track finished", re-resolve the incoming track and restart
     * it from zero, tearing down the fade that is already playing it.
     */
    private val isCrossfading: Boolean get() = crossfadePlayer != null

    /** Audio is flowing from either side of a fade. False once both are paused. */
    private val isPlayingNow: Boolean get() = player.isPlaying || crossfadePlayer?.isPlaying == true

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    sendSkipBroadcast(ACTION_SKIP_NEXT)
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    sendSkipBroadcast(ACTION_SKIP_PREVIOUS)
                    return SessionResult.RESULT_SUCCESS
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
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
    private var crossfadeProgress = 0f
    private var crossfadeRunnable: Runnable? = null
    private val sleepRunnable = Runnable {
        player.pause()
        crossfadePlayer?.pause()
    }
    private val crossfadeStartTimeout = Runnable {
        if (crossfadeRunnable == null) abortCrossfade()
    }
    private val stateTicker = object : Runnable {
        override fun run() {
            publishPlaybackState()
            if (isPlayingNow) stateHandler.postDelayed(this, STATE_TICK_INTERVAL_MS)
        }
    }
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishPlaybackState()
            updateNotification()
            // Mid-fade the incoming player carries the audio, so neither the position ticker nor
            // the visualizer is torn down just because the outgoing player stopped.
            stateHandler.removeCallbacks(stateTicker)
            if (isPlayingNow) stateHandler.post(stateTicker)
            else if (!isCrossfading) publishFftBands(FloatArray(FFT_BAND_COUNT))
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            if (!isUpdatingMetadata) {
                loadArtwork(mediaMetadata.artworkUri)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            publishPlaybackState()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            publishPlaybackState(hasError = true)
        }
    }
    private fun buildNotification(session: MediaSession): android.app.Notification {
        val playing = isPlayingNow
        return NotificationCompat.Builder(this, PLAYBACK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(currentArtworkBitmap)
            .setContentTitle(player.mediaMetadata.title ?: getString(R.string.app_name))
            .setContentText(player.mediaMetadata.artist)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_previous, getString(R.string.previous_track), controlIntent(ACTION_PREVIOUS, 1))
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                getString(if (playing) R.string.pause_playback else R.string.resume_playback),
                controlIntent(if (playing) ACTION_PAUSE else ACTION_RESUME, 2),
            )
            .addAction(android.R.drawable.ic_media_next, getString(R.string.next_track), controlIntent(ACTION_NEXT, 3))
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    private fun updateNotification() {
        session?.let {
            getSystemService(NotificationManager::class.java)
                .notify(PLAYBACK_NOTIFICATION_ID, buildNotification(it))
        }
    }

    private fun loadArtwork(uri: Uri?) {
        if (uri == null) {
            currentArtworkBitmap = null
            updateNotification()
            return
        }
        serviceScope.launch {
            val request = ImageRequest.Builder(this@NavaPlaybackService)
                .data(uri)
                .size(512, 512)
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build()
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                currentArtworkBitmap = bitmap

                // Update player metadata with artwork data for system player
                bitmap?.let { b ->
                    val stream = ByteArrayOutputStream()
                    b.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    val byteArray = stream.toByteArray()

                    isUpdatingMetadata = true
                    val currentItem = player.currentMediaItem
                    if (currentItem != null) {
                        val newMetadata = currentItem.mediaMetadata.buildUpon()
                            .setArtworkData(byteArray, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            .build()
                        val newItem = currentItem.buildUpon()
                            .setMediaMetadata(newMetadata)
                            .build()
                        player.replaceMediaItem(player.currentMediaItemIndex, newItem)
                    }
                    isUpdatingMetadata = false
                }

                updateNotification()
            }
        }
    }

    private fun controlIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, NavaPlaybackService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )

        cache = SimpleCache(
            File(cacheDir, "nava_audio_cache"),
            LeastRecentlyUsedCacheEvictor(128L * 1024L * 1024L),
            StandaloneDatabaseProvider(this),
        )
        sourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(this))

        val bundle = createPlayer()
        player = bundle.player
        activeAnalyzer = bundle.analyzer
        applyAudioAttributes(player, handleAudioFocus = true)
        player.addListener(playerListener)

        forwardingPlayer = createForwardingPlayer(player)

        session = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(mediaSessionCallback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                session: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                return MediaNotification(PLAYBACK_NOTIFICATION_ID, buildNotification(session))
            }

            override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean = false
        })
    }
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        val serviceResult = super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY_URI -> intent.getStringExtra(EXTRA_URI)?.let { uri ->
                cancelCrossfade()

                val title = intent.getStringExtra(EXTRA_TITLE)
                val artist = intent.getStringExtra(EXTRA_ARTIST)
                val artworkUri = intent.getStringExtra(EXTRA_ARTWORK_URI)

                currentTitle = title
                currentArtist = artist

                player.setMediaItem(mediaItem(uri, title, artist, artworkUri))
                player.prepare()
                player.play()

                // Force foreground with explicit notification update
                session?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            PLAYBACK_NOTIFICATION_ID,
                            buildNotification(it),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                    } else {
                        startForeground(PLAYBACK_NOTIFICATION_ID, buildNotification(it))
                    }
                }
            }
            ACTION_CROSSFADE_URI -> intent.getStringExtra(EXTRA_URI)?.let { uri ->
                startCrossfade(
                    uri = uri,
                    title = intent.getStringExtra(EXTRA_TITLE),
                    artist = intent.getStringExtra(EXTRA_ARTIST),
                    artworkUri = intent.getStringExtra(EXTRA_ARTWORK_URI),
                )
            }
            ACTION_PAUSE -> {
                player.pause()
                crossfadePlayer?.pause()
            }
            ACTION_RESUME -> {
                player.play()
                crossfadePlayer?.play()
            }
            ACTION_SEEK -> {
                // Scrubbing invalidates the fade that was scheduled for the old position.
                if (isCrossfading) abortCrossfade()
                player.seekTo(intent.getLongExtra(EXTRA_POSITION_MS, player.currentPosition))
            }
            ACTION_SPEED -> intent.getFloatExtra(EXTRA_SPEED, 1f).let { speed ->
                player.setPlaybackSpeed(speed)
                crossfadePlayer?.setPlaybackSpeed(speed)
            }
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
        player.release()
        session?.release()
        session = null
        cache.release()
        activeAnalyzer?.release()
        activeAnalyzer = null
        super.onDestroy()
    }

    /**
     * Every player gets its own FFT analyzer, and only the analyzer belonging to the player that
     * currently owns playback publishes bands. Without this the visualizer would go dark forever
     * after the first crossfade, because the promoted player had no analyzer attached to its sink.
     */
    private fun createPlayer(): PlayerBundle {
        var created: PlaybackFftAnalyzer? = null
        val analyzer = PlaybackFftAnalyzer { bands ->
            if (created === activeAnalyzer) publishFftBands(bands)
        }
        created = analyzer
        val fftProcessor = TeeAudioProcessor(analyzer)
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf<AudioProcessor>(fftProcessor))
                .build()
        }
        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(sourceFactory))
            .build()
        return PlayerBundle(exoPlayer, analyzer)
    }

    private fun applyAudioAttributes(target: ExoPlayer, handleAudioFocus: Boolean) {
        target.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            handleAudioFocus,
        )
    }

    private fun createForwardingPlayer(delegate: Player): ForwardingPlayer =
        object : ForwardingPlayer(delegate) {
            override fun isCommandAvailable(command: Int): Boolean =
                when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }

            override fun getAvailableCommands(): Player.Commands =
                super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()

            override fun seekToNext() { sendSkipBroadcast(ACTION_SKIP_NEXT) }
            override fun seekToPrevious() { sendSkipBroadcast(ACTION_SKIP_PREVIOUS) }
            override fun seekToNextMediaItem() { sendSkipBroadcast(ACTION_SKIP_NEXT) }
            override fun seekToPreviousMediaItem() { sendSkipBroadcast(ACTION_SKIP_PREVIOUS) }
        }

    private fun startCrossfade(uri: String, title: String?, artist: String?, artworkUri: String?) {
        if (isCrossfading || !player.isPlaying) return
        val bundle = createPlayer()
        val incoming = bundle.player
        crossfadePlayer = incoming
        crossfadeAnalyzer = bundle.analyzer
        crossfadeProgress = 0f
        // Audio focus stays owned by the outgoing player until the swap, so the incoming one must
        // not request it — a second request would be released again when the old player dies.
        applyAudioAttributes(incoming, handleAudioFocus = false)
        incoming.volume = 0f
        incoming.setPlaybackSpeed(player.playbackParameters.speed)
        incoming.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (crossfadePlayer === incoming) abortCrossfade()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Ramp only once the incoming track is really producing audio, otherwise the fade
                // would run its course against a still-buffering player and land on silence.
                if (isPlaying && crossfadePlayer === incoming) beginCrossfadeRamp(incoming)
            }
        })
        incoming.setMediaItem(mediaItem(uri, title, artist, artworkUri))
        incoming.prepare()
        incoming.playWhenReady = true
        crossfadeHandler.postDelayed(crossfadeStartTimeout, CROSSFADE_START_TIMEOUT_MS)
        // Publish immediately so the queue owner sees the masked state before the outgoing track ends.
        publishPlaybackState()
    }

    private fun beginCrossfadeRamp(incoming: ExoPlayer) {
        if (crossfadeRunnable != null) return
        crossfadeHandler.removeCallbacks(crossfadeStartTimeout)
        val runnable = object : Runnable {
            override fun run() {
                if (crossfadePlayer !== incoming) return
                // Advancing on ticks rather than wall clock means a pause mid-fade freezes the fade
                // instead of letting it run out silently while nothing is playing.
                val playing = isPlayingNow
                if (playing) {
                    crossfadeProgress += CROSSFADE_TICK_MS.toFloat() / CROSSFADE_DURATION_MS
                }
                val progress = crossfadeProgress.coerceIn(0f, 1f)
                player.volume = 1f - progress
                incoming.volume = progress
                if (progress >= 1f) finishCrossfade(incoming)
                else crossfadeHandler.postDelayed(
                    this,
                    if (playing) CROSSFADE_TICK_MS else CROSSFADE_PAUSED_TICK_MS,
                )
            }
        }
        crossfadeRunnable = runnable
        crossfadeHandler.post(runnable)
    }

    private fun finishCrossfade(incoming: ExoPlayer) {
        if (crossfadePlayer !== incoming) return
        crossfadeRunnable?.let(crossfadeHandler::removeCallbacks)
        crossfadeRunnable = null
        crossfadeHandler.removeCallbacks(crossfadeStartTimeout)

        val outgoing = player
        val outgoingAnalyzer = activeAnalyzer
        outgoing.removeListener(playerListener)

        player = incoming
        activeAnalyzer = crossfadeAnalyzer
        crossfadePlayer = null
        crossfadeAnalyzer = null
        crossfadeProgress = 0f

        outgoing.stop()
        outgoing.release()
        outgoingAnalyzer?.release()

        player.volume = 1f
        applyAudioAttributes(player, handleAudioFocus = true)
        player.addListener(playerListener)
        forwardingPlayer = createForwardingPlayer(player)
        session?.setPlayer(forwardingPlayer)

        currentTitle = player.mediaMetadata.title?.toString()
        currentArtist = player.mediaMetadata.artist?.toString()
        // The media item predates this listener, so onMediaMetadataChanged will not fire for it.
        currentArtworkBitmap = null
        loadArtwork(player.mediaMetadata.artworkUri)

        stateHandler.removeCallbacks(stateTicker)
        stateHandler.post(stateTicker)
        updateNotification()
        publishPlaybackState()
        sendBroadcast(Intent(ACTION_CROSSFADE_COMPLETE).setPackage(packageName))
    }

    /**
     * Drops the fade and tells the queue owner, so it can fall back to a normal advance. Publishing
     * after `crossfadePlayer` is cleared re-exposes a real `STATE_ENDED` instead of stalling.
     */
    private fun abortCrossfade() {
        if (!isCrossfading) return
        cancelCrossfade()
        sendBroadcast(Intent(ACTION_CROSSFADE_ABORTED).setPackage(packageName))
        publishPlaybackState()
    }

    private fun cancelCrossfade() {
        crossfadeRunnable?.let(crossfadeHandler::removeCallbacks)
        crossfadeRunnable = null
        crossfadeHandler.removeCallbacks(crossfadeStartTimeout)
        crossfadeProgress = 0f
        crossfadePlayer?.run {
            stop()
            release()
        }
        crossfadePlayer = null
        crossfadeAnalyzer?.release()
        crossfadeAnalyzer = null
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

    private fun sendSkipBroadcast(action: String) {
        sendBroadcast(Intent(action).setPackage(packageName))
    }

    private fun publishPlaybackState(hasError: Boolean = false) {
        val rawState = player.playbackState
        val reportedState =
            if (isCrossfading && rawState == Player.STATE_ENDED) Player.STATE_READY else rawState
        sendBroadcast(
            android.content.Intent(ACTION_PLAYBACK_STATE)
                .setPackage(packageName)
                .putExtra(EXTRA_PLAYING, isPlayingNow)
                .putExtra(EXTRA_POSITION_MS, player.currentPosition.coerceAtLeast(0L))
                .putExtra(EXTRA_DURATION_MS, player.duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L)
                .putExtra(EXTRA_PLAYBACK_STATE, reportedState)
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

    private class PlayerBundle(val player: ExoPlayer, val analyzer: PlaybackFftAnalyzer)

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
        const val ACTION_CROSSFADE_ABORTED = "com.example.nava.playback.CROSSFADE_ABORTED"
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
        const val CROSSFADE_DURATION_MS = 5_000L
        private const val STATE_TICK_INTERVAL_MS = 1_000L
        private const val CROSSFADE_TICK_MS = 50L
        private const val CROSSFADE_PAUSED_TICK_MS = 500L
        private const val CROSSFADE_START_TIMEOUT_MS = 8_000L
        private const val PLAYBACK_CHANNEL_ID = "nava_playback"
        private const val PLAYBACK_NOTIFICATION_ID = 1001
        private const val FFT_BAND_COUNT = 28
    }
}
