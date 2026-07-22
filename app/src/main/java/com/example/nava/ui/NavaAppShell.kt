package com.example.nava.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nava.R
import com.example.nava.domain.auth.AuthSession
import com.example.nava.domain.catalog.HomeTrack
import com.example.nava.domain.catalog.SearchTrack
import com.example.nava.domain.preferences.AppLanguage
import com.example.nava.domain.preferences.FontScale
import com.example.nava.domain.preferences.ThemeMode
import com.example.nava.domain.preferences.UserPreferences
import com.example.nava.ui.theme.NavaDimensions
import com.example.nava.ui.theme.NavaBlack
import com.example.nava.ui.theme.NavaSpacing
import com.example.nava.ui.theme.NavaWhite
import com.example.nava.ui.home.HomeUiState
import com.example.nava.ui.home.HomeViewModel
import com.example.nava.ui.search.SearchViewModel
import com.example.nava.ui.library.LibraryUiState
import com.example.nava.ui.library.LibraryViewModel
import com.example.nava.ui.library.LikesViewModel
import com.example.nava.ui.downloads.DownloadViewModel
import com.example.nava.ui.downloads.DownloadsUiState
import com.example.nava.ui.profile.ProfileViewModel
import com.example.nava.ui.social.SocialViewModel
import com.example.nava.ui.social.SocialPerson
import com.example.nava.ui.social.SocialSection
import com.example.nava.ui.chat.ChatShell
import com.example.nava.ui.chat.ChatViewModel
import com.example.nava.playback.NowPlaying
import com.example.nava.playback.PlaybackViewModel
import com.example.nava.ui.theme.NavaMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.math.abs

private data class NavItem(@StringRes val title: Int, val icon: ImageVector)

private val navigationItems = listOf(
    NavItem(R.string.home, Icons.Outlined.Home),
    NavItem(R.string.search, Icons.Outlined.ManageSearch),
    NavItem(R.string.downloads, Icons.Outlined.Download),
    NavItem(R.string.playlists, Icons.Outlined.QueueMusic),
    NavItem(R.string.profile, Icons.Outlined.AccountCircle),
)

@Composable
private fun NavaTopBarBrand() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
    ) {
        Surface(
            modifier = Modifier.size(NavaDimensions.HomeTopBarLogoSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.auth_logo_content_description),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = stringResource(R.string.top_bar_brand),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NavaAppShell(
    session: AuthSession,
    preferences: UserPreferences,
    onEvent: (NavaEvent) -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var socialOpen by rememberSaveable { mutableStateOf(false) }
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var queueCandidate by remember { mutableStateOf<HomeTrack?>(null) }
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val downloadViewModel: DownloadViewModel = hiltViewModel()
    val likesViewModel: LikesViewModel = hiltViewModel()
    val nowPlaying by playbackViewModel.nowPlaying.collectAsState()
    val playbackSpeed by playbackViewModel.playbackSpeed.collectAsState()
    val sleepTimerMinutes by playbackViewModel.sleepTimerMinutes.collectAsState()
    val fftBands by playbackViewModel.fftBands.collectAsState()
    val playbackError by playbackViewModel.playbackError.collectAsState()
    val downloadState by downloadViewModel.state.collectAsState()
    val downloadError by downloadViewModel.downloadError.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (settingsOpen) Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
                    else NavaTopBarBrand()
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.NotificationsNone, contentDescription = stringResource(R.string.notification))
                    }
                    IconButton(onClick = { settingsOpen = !settingsOpen }) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.open_settings))
                    }
                },
            )
        },
        bottomBar = {
            Column {
                nowPlaying?.let {
                    MiniPlayer(
                        nowPlaying = it,
                        onToggle = { if (it.playing) playbackViewModel.pause() else playbackViewModel.resume() },
                        onOpen = { playerExpanded = true },
                    )
                }
                NavigationBar {
                    navigationItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = index == selectedIndex,
                            onClick = { selectedIndex = index; settingsOpen = false; socialOpen = false },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.title)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            settingsOpen -> SettingsShell(preferences, onEvent, Modifier.padding(padding))
            socialOpen -> SocialShell(Modifier.padding(padding))
            selectedIndex == 0 -> HomeShell(
                modifier = Modifier.padding(padding),
                onPlay = playbackViewModel::play,
                onQueue = { queueCandidate = it },
                onShuffleSource = playbackViewModel::setShuffleSource,
            )
            selectedIndex == 1 -> SearchShell(
                modifier = Modifier.padding(padding),
                currentTrackId = nowPlaying?.track?.id,
                onTrackClick = { track ->
                    if (nowPlaying?.track?.id != track.id) playbackViewModel.play(track)
                    playerExpanded = true
                },
                onTrackOptions = { queueCandidate = it },
            )
            selectedIndex == 2 -> DownloadsShell(Modifier.padding(padding), downloadState, downloadViewModel)
            selectedIndex == 3 -> LibraryShell(modifier = Modifier.padding(padding), likesViewModel = likesViewModel)
            selectedIndex == 4 -> ProfileShell(session, Modifier.padding(padding), onDiscoverPeople = { socialOpen = true })
            else -> PlaceholderShell(navigationItems[selectedIndex].title, Modifier.padding(padding))
        }
    }
    nowPlaying?.let { now ->
        AnimatedVisibility(
            visible = playerExpanded,
            enter = fadeIn(tween(NavaMotion.Standard)) + slideInVertically(tween(NavaMotion.Standard)) { it },
            exit = fadeOut(tween(NavaMotion.Fast)) + slideOutVertically(tween(NavaMotion.Standard)) { it },
        ) {
            FullPlayer(
                nowPlaying = now,
                playbackSpeed = playbackSpeed,
                sleepTimerMinutes = sleepTimerMinutes,
                fftBands = fftBands,
                onDismiss = { playerExpanded = false },
                onToggle = { if (now.playing) playbackViewModel.pause() else playbackViewModel.resume() },
                onSeek = playbackViewModel::seekTo,
                onCycleSpeed = playbackViewModel::cycleSpeed,
                onCycleSleepTimer = playbackViewModel::cycleSleepTimer,
                onPrevious = playbackViewModel::skipToPrevious,
                onNext = playbackViewModel::skipToNext,
            )
        }
    }
    if (playbackError) {
        AlertDialog(
            onDismissRequest = playbackViewModel::clearPlaybackError,
            text = { Text(stringResource(R.string.playback_unavailable)) },
            confirmButton = {
                Button(onClick = playbackViewModel::clearPlaybackError) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
    queueCandidate?.let { track ->
        val isDownloaded = track.id in downloadState.downloadedTrackIds
        val isDownloading = track.id in downloadState.downloadingTrackIds
        val likes by likesViewModel.state.collectAsState()
        val isLiked = track.id in likes.likedIds
        ModalBottomSheet(
            onDismissRequest = { queueCandidate = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = NavaSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = NavaSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                ) {
                    AsyncImage(
                        model = track.coverImageUrl,
                        contentDescription = stringResource(R.string.track_artwork, track.title),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_foreground),
                        modifier = Modifier
                            .size(NavaDimensions.SearchActionArtworkSize)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = track.artistName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SongActionRow(
                    icon = Icons.Outlined.PlayArrow,
                    label = stringResource(R.string.play_now),
                    onClick = {
                        playbackViewModel.play(track)
                        playerExpanded = true
                        queueCandidate = null
                    },
                )
                SongActionRow(
                    icon = Icons.Outlined.QueueMusic,
                    label = stringResource(R.string.add_to_queue),
                    onClick = {
                        playbackViewModel.addToQueue(track)
                        queueCandidate = null
                    },
                )
                SongActionRow(
                    icon = if (isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    label = stringResource(if (isLiked) R.string.unlike_song else R.string.like_song),
                    onClick = {
                        likesViewModel.toggle(track)
                        queueCandidate = null
                    },
                )
                SongActionRow(
                    icon = Icons.Outlined.Download,
                    label = stringResource(
                        when {
                            isDownloaded -> R.string.downloaded
                            isDownloading -> R.string.downloading
                            else -> R.string.download
                        },
                    ),
                    enabled = !isDownloaded && !isDownloading,
                    onClick = {
                        downloadViewModel.request(track)
                        selectedIndex = 2
                        queueCandidate = null
                    },
                )
                Spacer(Modifier.height(NavaSpacing.Lg))
            }
        }
    }
    downloadError?.let { error ->
        AlertDialog(
            onDismissRequest = downloadViewModel::dismissDownloadError,
            text = { Text(error) },
            confirmButton = {
                Button(onClick = downloadViewModel::dismissDownloadError) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun SongActionRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Surface(
                modifier = Modifier.size(NavaDimensions.SearchActionIconSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(NavaSpacing.Sm),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MiniPlayer(nowPlaying: NowPlaying, onToggle: () -> Unit, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NavaSpacing.Lg, vertical = NavaSpacing.Sm),
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = NavaSpacing.Xs),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = nowPlaying.track.coverImageUrl,
                contentDescription = stringResource(R.string.now_playing_artwork),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(NavaDimensions.MiniPlayerArtworkSize)
                    .clip(MaterialTheme.shapes.medium),
            )
            Column(modifier = Modifier.weight(1f).padding(start = NavaSpacing.Sm)) {
                Text(
                    nowPlaying.track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    nowPlaying.track.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Surface(
                onClick = onToggle,
                modifier = Modifier.size(NavaDimensions.PlayerSecondaryControlSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (nowPlaying.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(if (nowPlaying.playing) R.string.pause_playback else R.string.resume_playback),
                    )
                }
            }
        }
    }
}

@Composable
private fun FullPlayer(
    nowPlaying: NowPlaying,
    playbackSpeed: Float,
    sleepTimerMinutes: Long?,
    fftBands: FloatArray,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val fallbackPaletteColor = MaterialTheme.colorScheme.primary
    var paletteColor by remember(nowPlaying.track.id) { mutableStateOf(fallbackPaletteColor) }
    var scrubPosition by rememberSaveable(nowPlaying.track.id) { mutableStateOf(nowPlaying.positionMs.toFloat()) }
    var scrubbing by rememberSaveable(nowPlaying.track.id) { mutableStateOf(false) }
    var dragX by remember(nowPlaying.track.id) { mutableFloatStateOf(0f) }
    var dragY by remember(nowPlaying.track.id) { mutableFloatStateOf(0f) }
    val swipeThreshold = with(LocalDensity.current) { NavaDimensions.PlayerSwipeThreshold.toPx() }
    val speedLabel = remember(playbackSpeed) { DecimalFormat("0.##").format(playbackSpeed) }
    LaunchedEffect(nowPlaying.positionMs, scrubbing) {
        if (!scrubbing) scrubPosition = nowPlaying.positionMs.toFloat()
    }
    val durationMs = nowPlaying.durationMs.coerceAtLeast(1L)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            paletteColor.copy(alpha = .58f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = NavaSpacing.Xl),
            ) {
                PlayerHeader(onDismiss)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = NavaSpacing.Md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = dragX * .45f
                                translationY = dragY.coerceAtLeast(0f) * .35f
                                rotationZ = (dragX / swipeThreshold).coerceIn(-1f, 1f) * 3f
                                alpha = (1f - (abs(dragX) + dragY.coerceAtLeast(0f)) / (swipeThreshold * 5f))
                                    .coerceIn(.72f, 1f)
                            }
                            .pointerInput(nowPlaying.track.id, swipeThreshold) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragX = 0f
                                        dragY = 0f
                                    },
                                    onDragCancel = {
                                        dragX = 0f
                                        dragY = 0f
                                    },
                                    onDragEnd = {
                                        when {
                                            abs(dragX) > abs(dragY) && abs(dragX) >= swipeThreshold -> {
                                                if (dragX > 0f) onPrevious() else onNext()
                                            }
                                            dragY >= swipeThreshold -> onDismiss()
                                        }
                                        dragX = 0f
                                        dragY = 0f
                                    },
                                ) { change, dragAmount ->
                                    change.consume()
                                    dragX += dragAmount.x
                                    dragY += dragAmount.y
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs),
                    ) {
                        Text(
                            text = nowPlaying.track.title,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = nowPlaying.track.artistName,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(NavaSpacing.Sm))
                        NowPlayingArtwork(
                            nowPlaying = nowPlaying,
                            onPaletteColorChanged = { paletteColor = it },
                        )
                        Text(
                            text = stringResource(R.string.player_swipe_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    EqualizerVisualizer(bands = fftBands, isPlaying = nowPlaying.playing)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = scrubPosition.coerceIn(0f, durationMs.toFloat()),
                            onValueChange = { value ->
                                scrubbing = true
                                scrubPosition = value
                            },
                            onValueChangeFinished = {
                                onSeek(scrubPosition.toLong())
                                scrubbing = false
                            },
                            valueRange = 0f..durationMs.toFloat(),
                        )
                        Text(
                            text = stringResource(
                                R.string.playback_position,
                                playbackMinutes(scrubPosition.toLong()),
                                playbackSeconds(scrubPosition.toLong()),
                                playbackMinutes(durationMs),
                                playbackSeconds(durationMs),
                            ),
                            modifier = Modifier.align(Alignment.End),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PlayerTransportControls(
                        playing = nowPlaying.playing,
                        onPrevious = onPrevious,
                        onToggle = onToggle,
                        onNext = onNext,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                    ) {
                        PlayerUtilityButton(
                            icon = Icons.Outlined.Speed,
                            label = stringResource(R.string.playback_speed_value, speedLabel),
                            onClick = onCycleSpeed,
                            modifier = Modifier.weight(1f),
                        )
                        PlayerUtilityButton(
                            icon = Icons.Outlined.Timer,
                            label = sleepTimerMinutes?.let { stringResource(R.string.sleep_timer_minutes, it) }
                                ?: stringResource(R.string.sleep_timer_off),
                            onClick = onCycleSleepTimer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(NavaDimensions.PlayerSecondaryControlSize),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss, modifier = Modifier.size(NavaDimensions.PlayerSecondaryControlSize)) {
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(R.string.collapse_player),
                modifier = Modifier.size(NavaSpacing.Xxl),
            )
        }
        Text(
            text = stringResource(R.string.now_playing),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(NavaDimensions.PlayerSecondaryControlSize))
    }
}

@Composable
private fun PlayerTransportControls(
    playing: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerControlButton(Icons.Outlined.SkipPrevious, R.string.previous_track, onPrevious)
        PlayerControlButton(
            icon = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = if (playing) R.string.pause_playback else R.string.resume_playback,
            onClick = onToggle,
            primary = true,
        )
        PlayerControlButton(Icons.Outlined.SkipNext, R.string.next_track, onNext)
    }
}

@Composable
private fun PlayerControlButton(
    icon: ImageVector,
    @StringRes contentDescription: Int,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(if (primary) NavaDimensions.PlayerPrimaryControlSize else NavaDimensions.PlayerSecondaryControlSize),
        shape = CircleShape,
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = stringResource(contentDescription),
                modifier = Modifier.size(if (primary) NavaSpacing.Xxl else NavaSpacing.Xl),
            )
        }
    }
}

@Composable
private fun PlayerUtilityButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(NavaDimensions.PlayerUtilityControlHeight),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = NavaSpacing.Md),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(NavaSpacing.Xl))
            Spacer(Modifier.size(NavaSpacing.Sm))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EqualizerVisualizer(bands: FloatArray, isPlaying: Boolean) {
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(NavaDimensions.PlayerVisualizerHeight)) {
        val barCount = bands.size.coerceAtLeast(1)
        val slotWidth = size.width / barCount
        val barWidth = slotWidth * .46f
        repeat(barCount) { index ->
            val spectrumLevel = bands.getOrElse(index) { 0f }.coerceIn(0f, 1f)
            val level = if (isPlaying) .08f + spectrumLevel * .9f else .08f
            val barHeight = size.height * level
            drawRoundRect(
                color = barColor,
                topLeft = Offset(slotWidth * index + (slotWidth - barWidth) / 2f, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

private fun playbackMinutes(timeMs: Long): Long = timeMs / 60_000L

private fun playbackSeconds(timeMs: Long): Long = (timeMs / 1_000L) % 60L

@Composable
private fun NowPlayingArtwork(
    nowPlaying: NowPlaying,
    onPaletteColorChanged: (Color) -> Unit,
) {
    val fallbackPaletteColor = MaterialTheme.colorScheme.primary
    var bitmap by remember(nowPlaying.track.coverImageUrl) { mutableStateOf<Bitmap?>(null) }
    val coverRotation = remember(nowPlaying.track.id) { Animatable(0f) }
    LaunchedEffect(nowPlaying.track.id, nowPlaying.playing) {
        if (!nowPlaying.playing) {
            coverRotation.stop()
            return@LaunchedEffect
        }
        while (isActive) {
            coverRotation.animateTo(
                targetValue = coverRotation.value + 360f,
                animationSpec = tween(NavaMotion.Slow * 24, easing = LinearEasing),
            )
        }
    }
    LaunchedEffect(bitmap) {
        bitmap?.let { cover ->
            val paletteColor = withContext(Dispatchers.Default) {
                val softwareBitmap = cover.copy(Bitmap.Config.ARGB_8888, false)
                Color(Palette.from(softwareBitmap).generate().getVibrantColor(fallbackPaletteColor.toArgb()))
            }
            onPaletteColorChanged(paletteColor)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = NavaDimensions.PlayerArtworkMaxSize)
            .aspectRatio(1f)
            .clip(CircleShape)
            .border(
                NavaDimensions.PlayerArtworkBorderWidth,
                MaterialTheme.colorScheme.onBackground.copy(alpha = .18f),
                CircleShape,
            )
            .padding(NavaSpacing.Sm),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = nowPlaying.track.coverImageUrl,
            contentDescription = stringResource(R.string.now_playing_artwork),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .rotate(coverRotation.value),
            onSuccess = { result ->
                bitmap = (result.result.drawable as? BitmapDrawable)?.bitmap
            },
        )
        Box(
            modifier = Modifier
                .size(NavaSpacing.Xxl)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .9f))
                .border(
                    NavaDimensions.PlayerArtworkBorderWidth,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = .25f),
                    CircleShape,
                ),
        )
    }
}

@Composable private fun LibraryShell(modifier: Modifier, viewModel: LibraryViewModel = hiltViewModel(), likesViewModel: LikesViewModel) {
 val state by viewModel.state.collectAsState()
 val likes by likesViewModel.state.collectAsState()
 Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
  Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineSmall)
  when (val current = state) {
   LibraryUiState.Loading -> CircularProgressIndicator()
   LibraryUiState.Error -> { Text(stringResource(R.string.library_error)); Button(onClick = viewModel::reload) { Text(stringResource(R.string.retry)) } }
   is LibraryUiState.Content -> {
    Text(stringResource(R.string.liked_count, likes.songs.size.toLong()), style = MaterialTheme.typography.titleMedium)
    if (likes.songs.isEmpty()) Text(stringResource(R.string.liked_songs_empty)) else LazyColumn(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) { items(likes.songs, key = { it.id }) { song -> Card { Row(Modifier.fillMaxWidth().padding(NavaSpacing.Md), verticalAlignment = Alignment.CenterVertically) { Text(song.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Button(onClick = { likesViewModel.remove(song.id) }) { Text(stringResource(R.string.unlike_song)) } } } } }
    if (current.summary.playlists.isEmpty()) Text(stringResource(R.string.library_empty)) else LazyColumn(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) { items(current.summary.playlists, key = { it.id }) { playlist -> Card { Column(Modifier.padding(NavaSpacing.Md)) { Text(playlist.title, style = MaterialTheme.typography.titleMedium); playlist.description?.let { Text(it) } } } } }
   }
  }
 }
}

@Composable
private fun SearchShell(
    modifier: Modifier,
    currentTrackId: String?,
    onTrackClick: (HomeTrack) -> Unit,
    onTrackOptions: (HomeTrack) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = NavaSpacing.Lg, vertical = NavaSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
        ) {
            Text(
                text = stringResource(R.string.search_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.search_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                placeholder = { Text(stringResource(R.string.search_catalog)) },
                leadingIcon = { Icon(Icons.Outlined.ManageSearch, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                SearchFilterChip(
                    selected = state.language == null,
                    label = R.string.filter_all,
                    onClick = { viewModel.setLanguage(null) },
                )
                SearchFilterChip(
                    selected = state.language == "en",
                    label = R.string.filter_english,
                    onClick = { viewModel.setLanguage("en") },
                )
                SearchFilterChip(
                    selected = state.language == "fa",
                    label = R.string.filter_persian,
                    onClick = { viewModel.setLanguage("fa") },
                )
            }
        }
        when {
            state.query.isBlank() -> SearchMessage(
                title = stringResource(R.string.search_start_title),
                body = stringResource(R.string.search_start_subtitle),
            )
            state.loading && state.results.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            state.failed -> SearchMessage(
                title = stringResource(R.string.search_error),
                body = stringResource(R.string.search_error_hint),
                action = { Button(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) } },
            )
            state.results.isEmpty() -> SearchMessage(
                title = stringResource(R.string.search_empty),
                body = stringResource(R.string.search_empty_hint),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = NavaSpacing.Lg,
                    end = NavaSpacing.Lg,
                    top = NavaSpacing.Xs,
                    bottom = NavaSpacing.Xl,
                ),
                verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.search_results_count, state.results.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = NavaSpacing.Xs),
                    )
                }
                if (state.loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                items(state.results, key = SearchTrack::id) { track ->
                    SearchTrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        onClick = {
                            focusManager.clearFocus()
                            onTrackClick(track.toHomeTrack())
                        },
                        onOptions = {
                            focusManager.clearFocus()
                            onTrackOptions(track.toHomeTrack())
                        },
                    )
                }
                if (state.canLoadMore) item {
                    LaunchedEffect(state.results.size) { viewModel.loadMore() }
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterChip(selected: Boolean, @StringRes label: Int, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = stringResource(label),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        },
    )
}

@Composable
private fun SearchMessage(
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(NavaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(NavaDimensions.SearchEmptyIconSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.ManageSearch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(NavaDimensions.SearchEmptyGlyphSize),
                )
            }
        }
        Spacer(Modifier.height(NavaSpacing.Lg))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(NavaSpacing.Xs))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.let {
            Spacer(Modifier.height(NavaSpacing.Lg))
            it()
        }
    }
}

@Composable
private fun SearchTrackRow(
    track: SearchTrack,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onOptions: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = NavaSpacing.Xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.coverImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.track_artwork, track.title),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    modifier = Modifier
                        .size(NavaDimensions.SearchTrackArtworkSize)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(NavaDimensions.SearchPlayBadgeSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = NavaSpacing.Xs,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(R.string.play_track, track.title),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(NavaSpacing.Xs),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${track.genre} • ${formatTrackDuration(track.durationSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onOptions) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.track_more_options, track.title),
                )
            }
        }
    }
}

private fun formatTrackDuration(durationSeconds: Int): String =
    "${durationSeconds / 60}:${(durationSeconds % 60).toString().padStart(2, '0')}"

@Composable
private fun HomeShell(
    modifier: Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onPlay: (HomeTrack) -> Unit,
    onQueue: (HomeTrack) -> Unit,
    onShuffleSource: (List<HomeTrack>) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    (state as? HomeUiState.Content)?.feed?.let { feed ->
        LaunchedEffect(feed) {
            onShuffleSource(
                (feed.featured + feed.trending + feed.newest + feed.global + feed.local)
                    .distinctBy(HomeTrack::id),
            )
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xl),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = NavaSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs),
            ) {
                Text(
                    stringResource(R.string.home_welcome),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.home_welcome_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (val current = state) {
            HomeUiState.Loading -> item { HomeLoading() }
            HomeUiState.Error -> item { HomeError(onRetry = viewModel::reload) }
            is HomeUiState.Content -> homeContent(current, onPlay, onQueue)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeContent(
    state: HomeUiState.Content,
    onPlay: (HomeTrack) -> Unit,
    onQueue: (HomeTrack) -> Unit,
) {
    if (
        state.feed.featured.isEmpty() &&
        state.feed.trending.isEmpty() &&
        state.feed.newest.isEmpty() &&
        state.feed.global.isEmpty() &&
        state.feed.local.isEmpty()
    ) {
        item {
            Text(
                stringResource(R.string.catalog_empty),
                modifier = Modifier.padding(horizontal = NavaSpacing.Lg),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }
    item {
        Text(
            text = stringResource(R.string.home_featured),
            modifier = Modifier.padding(horizontal = NavaSpacing.Lg),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
    item {
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NavaSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            items(state.feed.featured, key = HomeTrack::id) { FeaturedCard(it, onPlay, onQueue) }
        }
    }
    item {
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NavaSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            item { QuickAction(R.string.quick_liked, Icons.Outlined.FavoriteBorder) }
            item { QuickAction(R.string.quick_recent, Icons.Outlined.History) }
            item { QuickAction(R.string.quick_playlists, Icons.Outlined.QueueMusic) }
            item { QuickAction(R.string.quick_artists, Icons.Outlined.Groups) }
        }
    }
    item { DiscoverySection(R.string.home_trending, state.feed.trending, onPlay, onQueue) }
    item { DiscoverySection(R.string.home_newest, state.feed.newest, onPlay, onQueue) }
    item { DiscoverySection(R.string.home_global, state.feed.global, onPlay, onQueue) }
    item { DiscoverySection(R.string.home_local, state.feed.local, onPlay, onQueue) }
}

@Composable
private fun HomeLoading() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { CircularProgressIndicator() }
}

@Composable
private fun HomeError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
    ) {
        Text(stringResource(R.string.home_load_error), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun DownloadsShell(modifier: Modifier, state: DownloadsUiState, viewModel: DownloadViewModel) {
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
        Text(stringResource(R.string.downloads), style = MaterialTheme.typography.headlineSmall)
        if (state.downloads.isEmpty() && state.activeDownloads.isEmpty()) {
            Text(stringResource(R.string.downloads_empty))
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
            items(state.activeDownloads, key = { "active-${it.trackId}" }) { track ->
                Card {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md),
                        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                    ) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium)
                        Text(track.artistName, style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { track.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(stringResource(R.string.download_progress, track.progressPercent), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            items(state.downloads, key = { it.trackId }) { track ->
                val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                    if (value != SwipeToDismissBoxValue.Settled) viewModel.remove(track)
                    value != SwipeToDismissBoxValue.Settled
                })
                SwipeToDismissBox(state = dismissState, backgroundContent = { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.errorContainer) {} }) {
                    Card { Row(Modifier.fillMaxWidth().padding(NavaSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(track.title, style = MaterialTheme.typography.titleMedium); Text(track.artistName) }
                        Button(onClick = { viewModel.remove(track) }) { Text(stringResource(R.string.remove_download)) }
                    } }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    track: HomeTrack,
    onPlay: (HomeTrack) -> Unit,
    onQueue: (HomeTrack) -> Unit,
) {
    val description = stringResource(R.string.track_card_description, track.title, track.artistName)
    val queueLabel = stringResource(R.string.queue_track_accessibility, track.title)
    Card(
        modifier = Modifier
            .width(NavaDimensions.HomeFeaturedCardWidth)
            .height(NavaDimensions.HomeFeaturedCardHeight)
            .semantics {
                contentDescription = description
                role = Role.Button
                onLongClick(label = queueLabel) {
                    onQueue(track)
                    true
                }
            }
            .combinedClickable(onClick = { onPlay(track) }, onLongClick = { onQueue(track) }),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = NavaSpacing.Xs),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = track.coverImageUrl,
                contentDescription = stringResource(R.string.track_artwork, track.title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, NavaBlack.copy(alpha = .9f)),
                        ),
                    ),
            )
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(NavaSpacing.Md),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(
                    stringResource(R.string.featured_badge),
                    modifier = Modifier.padding(horizontal = NavaSpacing.Md, vertical = NavaSpacing.Xs),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(NavaSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs),
            ) {
                Text(track.title, style = MaterialTheme.typography.titleLarge, color = NavaWhite, fontWeight = FontWeight.Bold)
                Text(track.artistName, style = MaterialTheme.typography.bodyMedium, color = NavaWhite.copy(alpha = .82f))
                Text(stringResource(R.string.home_carousel_caption), style = MaterialTheme.typography.labelMedium, color = NavaWhite.copy(alpha = .7f))
            }
        }
    }
}

@Composable
private fun DiscoverySection(
    @StringRes title: Int,
    tracks: List<HomeTrack>,
    onPlay: (HomeTrack) -> Unit,
    onQueue: (HomeTrack) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
        Text(
            stringResource(title),
            modifier = Modifier.padding(horizontal = NavaSpacing.Lg),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NavaSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            items(tracks, key = HomeTrack::id) { track ->
                val description = stringResource(R.string.track_card_description, track.title, track.artistName)
                val queueLabel = stringResource(R.string.queue_track_accessibility, track.title)
                Card(
                    modifier = Modifier
                        .width(NavaDimensions.HomeTrackCardWidth)
                        .semantics {
                            contentDescription = description
                            role = Role.Button
                            onLongClick(label = queueLabel) {
                                onQueue(track)
                                true
                            }
                        }
                        .combinedClickable(
                            onClick = { onPlay(track) },
                            onLongClick = { onQueue(track) },
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column {
                        AsyncImage(
                            model = track.coverImageUrl,
                            contentDescription = stringResource(R.string.track_artwork, track.title),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(NavaDimensions.HomeTrackArtworkHeight),
                        )
                        Column(
                            modifier = Modifier.padding(NavaSpacing.Md),
                            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs),
                        ) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                                track.artistName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(@StringRes label: Int, icon: ImageVector) {
    Surface(
        onClick = {},
        modifier = Modifier
            .width(NavaDimensions.HomeQuickActionWidth)
            .height(NavaDimensions.HomeQuickActionHeight),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(NavaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Surface(
                modifier = Modifier.size(NavaDimensions.HomeTopBarLogoSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(NavaSpacing.Xl))
                }
            }
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PlaceholderShell(@StringRes title: Int, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.shell_placeholder), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileShell(
    session: AuthSession,
    modifier: Modifier,
    onDiscoverPeople: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::uploadAvatar)
    }
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
            Button(onClick = { avatarPicker.launch("image/*") }, enabled = !state.isSaving) {
                state.avatarUrl?.let { url ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(url)
                            .memoryCacheKey(state.avatarPath)
                            .diskCacheKey(state.avatarPath)
                            .build(),
                        contentDescription = stringResource(R.string.user_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(NavaSpacing.Xxl).clip(CircleShape),
                    )
                } ?: Icon(Icons.Outlined.AccountCircle, contentDescription = stringResource(R.string.user_avatar), modifier = Modifier.size(NavaSpacing.Xxl))
            }
            Column {
                Text(if (state.isLoading) stringResource(R.string.profile_loading) else state.displayName, style = MaterialTheme.typography.titleLarge)
                Text(session.email, style = MaterialTheme.typography.bodyMedium)
                AssistChip(onClick = {}, label = { Text(stringResource(if (state.isPremium) R.string.premium else R.string.standard)) })
            }
        }
        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::changeDisplayName,
            enabled = !state.isLoading && !state.isSaving,
            label = { Text(stringResource(R.string.display_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = viewModel::saveProfile, enabled = !state.isLoading && !state.isSaving) {
            if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(NavaSpacing.Md))
            else Text(stringResource(R.string.save_profile))
        }
        if (!state.isPremium) Button(onClick = viewModel::upgrade, enabled = !state.isSaving) { Text(stringResource(R.string.upgrade_demo)) }
        Button(onClick = onDiscoverPeople) { Text(stringResource(R.string.discover_people)) }
    }
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            text = { Text(error) },
            confirmButton = { Button(onClick = viewModel::dismissError) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun SocialShell(modifier: Modifier, viewModel: SocialViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val chatViewModel: ChatViewModel = hiltViewModel()
    var messagesOpen by rememberSaveable { mutableStateOf(false) }
    if (messagesOpen) {
        ChatShell(modifier = modifier, onBack = { messagesOpen = false }, viewModel = chatViewModel)
        return
    }
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
        Text(stringResource(R.string.people), style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { messagesOpen = true }) { Text(stringResource(R.string.messages)) }
        Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
            AssistChip(onClick = { viewModel.select(SocialSection.PEOPLE) }, label = { Text(stringResource(R.string.people)) })
            AssistChip(onClick = { viewModel.select(SocialSection.FOLLOWING) }, label = { Text(stringResource(R.string.following)) })
            AssistChip(onClick = { viewModel.select(SocialSection.FOLLOWERS) }, label = { Text(stringResource(R.string.followers)) })
            AssistChip(onClick = { viewModel.select(SocialSection.PLAYLISTS) }, label = { Text(stringResource(R.string.public_playlists)) })
        }
        if (state.section == SocialSection.PEOPLE) OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::search,
            label = { Text(stringResource(R.string.search_people)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            state.loading -> CircularProgressIndicator()
            state.error -> Button(onClick = { viewModel.select(state.section) }) { Text(stringResource(R.string.retry)) }
            state.section == SocialSection.PLAYLISTS && state.playlists.isEmpty() -> Text(stringResource(R.string.playlists_empty))
            state.people.isEmpty() && state.section != SocialSection.PLAYLISTS -> Text(stringResource(R.string.people_empty))
            state.section == SocialSection.PLAYLISTS -> LazyColumn(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                items(state.playlists, key = { it.id }) { playlist -> Card { Column(Modifier.padding(NavaSpacing.Md)) { Text(playlist.title, style = MaterialTheme.typography.titleMedium); Text(playlist.ownerName); playlist.description?.let { Text(it) }; Text(stringResource(R.string.track_count, playlist.trackCount)) } } }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                items(state.people, key = SocialPerson::id) { person ->
                    Card {
                        Row(Modifier.fillMaxWidth().padding(NavaSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(person.displayName, style = MaterialTheme.typography.titleMedium) }
                            Button(onClick = { viewModel.toggleFollow(person) }) {
                                Text(stringResource(if (person.isFollowing) R.string.unfollow else R.string.follow))
                            }
                            IconButton(onClick = { chatViewModel.open(person); messagesOpen = true }) {
                                Icon(Icons.Outlined.Chat, contentDescription = stringResource(R.string.open_chat))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsShell(
    preferences: UserPreferences,
    onEvent: (NavaEvent) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg)) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
        SettingButtons(
            title = R.string.theme,
            options = listOf(
                R.string.theme_system to ThemeMode.SYSTEM,
                R.string.theme_light to ThemeMode.LIGHT,
                R.string.theme_dark to ThemeMode.DARK,
            ),
            selected = preferences.themeMode,
            onSelected = { onEvent(NavaEvent.SetTheme(it)) },
        )
        SettingButtons(
            title = R.string.language,
            options = listOf(
                R.string.language_english to AppLanguage.ENGLISH,
                R.string.language_persian to AppLanguage.PERSIAN,
            ),
            selected = preferences.language,
            onSelected = { onEvent(NavaEvent.SetLanguage(it)) },
        )
        SettingButtons(
            title = R.string.font_size,
            options = listOf(
                R.string.font_size_small to FontScale.SMALL,
                R.string.font_size_standard to FontScale.STANDARD,
                R.string.font_size_large to FontScale.LARGE,
            ),
            selected = preferences.fontScale,
            onSelected = { onEvent(NavaEvent.SetFontScale(it)) },
        )
        AssistChip(onClick = { onEvent(NavaEvent.SignOut) }, label = { Text(stringResource(R.string.sign_out)) })
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T> SettingButtons(
    @StringRes title: Int,
    options: List<Pair<Int, T>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm), modifier = Modifier.fillMaxWidth()) {
            options.forEach { (label, value) ->
                Surface(
                    onClick = { onSelected(value) },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(stringResource(label), modifier = Modifier.padding(NavaSpacing.Md))
                }
            }
        }
    }
}
