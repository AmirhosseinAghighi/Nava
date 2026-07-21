package com.example.nava.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.example.nava.R
import com.example.nava.domain.auth.AuthSession
import com.example.nava.domain.catalog.HomeTrack
import com.example.nava.domain.preferences.AppLanguage
import com.example.nava.domain.preferences.FontScale
import com.example.nava.domain.preferences.ThemeMode
import com.example.nava.domain.preferences.UserPreferences
import com.example.nava.ui.theme.NavaSpacing
import com.example.nava.ui.home.HomeUiState
import com.example.nava.ui.home.HomeViewModel
import com.example.nava.ui.search.SearchViewModel
import com.example.nava.ui.library.LibraryUiState
import com.example.nava.ui.library.LibraryViewModel
import com.example.nava.ui.downloads.DownloadViewModel
import com.example.nava.ui.downloads.DownloadsUiState
import com.example.nava.ui.profile.ProfileViewModel
import com.example.nava.playback.NowPlaying
import com.example.nava.playback.PlaybackViewModel
import com.example.nava.ui.theme.NavaMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private data class NavItem(@StringRes val title: Int, val icon: ImageVector)

private val navigationItems = listOf(
    NavItem(R.string.home, Icons.Outlined.Home),
    NavItem(R.string.search, Icons.Outlined.ManageSearch),
    NavItem(R.string.downloads, Icons.Outlined.Download),
    NavItem(R.string.playlists, Icons.Outlined.QueueMusic),
    NavItem(R.string.profile, Icons.Outlined.AccountCircle),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NavaAppShell(
    session: AuthSession,
    preferences: UserPreferences,
    onEvent: (NavaEvent) -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var queueCandidate by remember { mutableStateOf<HomeTrack?>(null) }
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val downloadViewModel: DownloadViewModel = hiltViewModel()
    val nowPlaying by playbackViewModel.nowPlaying.collectAsState()
    val playbackError by playbackViewModel.playbackError.collectAsState()
    val downloadState by downloadViewModel.state.collectAsState()
    val downloadError by downloadViewModel.downloadError.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.top_bar_brand), style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.NotificationsNone, contentDescription = stringResource(R.string.notification))
                    }
                    IconButton(onClick = { selectedIndex = navigationItems.lastIndex }) {
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
                            onClick = { selectedIndex = index },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.title)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (selectedIndex) {
            0 -> HomeShell(
                modifier = Modifier.padding(padding),
                onPlay = playbackViewModel::play,
                onQueue = { queueCandidate = it },
                onShuffleSource = playbackViewModel::setShuffleSource,
            )
            1 -> SearchShell(modifier = Modifier.padding(padding))
            2 -> DownloadsShell(Modifier.padding(padding), downloadState, downloadViewModel)
            3 -> LibraryShell(modifier = Modifier.padding(padding))
            4 -> ProfileShell(session, preferences, onEvent, Modifier.padding(padding))
            else -> PlaceholderShell(navigationItems[selectedIndex].title, Modifier.padding(padding))
        }
    }
    nowPlaying?.let { now ->
        AnimatedVisibility(
            visible = playerExpanded,
            enter = fadeIn(tween(NavaMotion.Standard)) + scaleIn(tween(NavaMotion.Standard)),
            exit = fadeOut(tween(NavaMotion.Fast)) + scaleOut(tween(NavaMotion.Fast)),
        ) {
            FullPlayer(
                nowPlaying = now,
                onDismiss = { playerExpanded = false },
                onToggle = { if (now.playing) playbackViewModel.pause() else playbackViewModel.resume() },
                onSeek = playbackViewModel::seekTo,
                onSpeed = playbackViewModel::setSpeed,
                onSleep = playbackViewModel::setSleepTimer,
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
        AlertDialog(
            onDismissRequest = { queueCandidate = null },
            title = { Text(stringResource(R.string.song_actions)) },
            text = { Text("${track.title}\n${track.artistName}") },
            confirmButton = {
                Button(onClick = {
                    playbackViewModel.addToQueue(track)
                    queueCandidate = null
                }) {
                    Text(stringResource(R.string.add_to_queue))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                    Button(
                        enabled = !isDownloaded && !isDownloading,
                        onClick = {
                            downloadViewModel.request(track)
                            selectedIndex = 2
                            queueCandidate = null
                        },
                    ) {
                        Text(
                            stringResource(
                                when {
                                    isDownloaded -> R.string.downloaded
                                    isDownloading -> R.string.downloading
                                    else -> R.string.download
                                },
                            ),
                        )
                    }
                    Button(onClick = { queueCandidate = null }) { Text(stringResource(R.string.close)) }
                }
            },
        )
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
private fun MiniPlayer(nowPlaying: NowPlaying, onToggle: () -> Unit, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = nowPlaying.track.coverImageUrl,
                contentDescription = stringResource(R.string.now_playing_artwork),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(NavaSpacing.Xxl)
                    .clip(MaterialTheme.shapes.medium),
            )
            Column(modifier = Modifier.weight(1f).padding(start = NavaSpacing.Sm)) {
                Text(nowPlaying.track.title, style = MaterialTheme.typography.titleMedium)
                Text(nowPlaying.track.artistName, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (nowPlaying.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(if (nowPlaying.playing) R.string.pause_playback else R.string.resume_playback),
                )
            }
        }
    }
}

@Composable
private fun FullPlayer(
    nowPlaying: NowPlaying,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onSleep: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val pulse by animateFloatAsState(targetValue = if (nowPlaying.playing) 1f else .35f, animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "visualizer")
    var scrubPosition by rememberSaveable(nowPlaying.track.id) { mutableStateOf(nowPlaying.positionMs.toFloat()) }
    var scrubbing by rememberSaveable(nowPlaying.track.id) { mutableStateOf(false) }
    LaunchedEffect(nowPlaying.positionMs, scrubbing) {
        if (!scrubbing) scrubPosition = nowPlaying.positionMs.toFloat()
    }
    val durationMs = nowPlaying.durationMs.coerceAtLeast(1L)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(nowPlaying.track.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
                Text(nowPlaying.track.artistName, style = MaterialTheme.typography.bodyLarge)
                NowPlayingArtwork(nowPlaying)
                Canvas(modifier = Modifier.fillMaxWidth().size(NavaSpacing.Xxl)) {
                    val barWidth = size.width / 11f
                    repeat(8) { index ->
                        val height = size.height * (0.2f + ((index % 3) * .18f) + pulse * .22f)
                        drawRect(Color(0xFFFCA311), topLeft = androidx.compose.ui.geometry.Offset(barWidth * (index + 1), size.height - height), size = androidx.compose.ui.geometry.Size(barWidth * .5f, height))
                    }
                }
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
                    stringResource(
                        R.string.playback_position,
                        playbackMinutes(scrubPosition.toLong()),
                        playbackSeconds(scrubPosition.toLong()),
                        playbackMinutes(durationMs),
                        playbackSeconds(durationMs),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            Icons.Outlined.SkipPrevious,
                            contentDescription = stringResource(R.string.previous_track),
                        )
                    }
                    IconButton(onClick = onToggle) {
                        Icon(
                            if (nowPlaying.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(
                                if (nowPlaying.playing) R.string.pause_playback else R.string.resume_playback,
                            ),
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.Outlined.SkipNext,
                            contentDescription = stringResource(R.string.next_track),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                    AssistChip(onClick = { onSpeed(0.75f) }, label = { Text("0.75×") })
                    AssistChip(onClick = { onSpeed(1f) }, label = { Text("1×") })
                    AssistChip(onClick = { onSpeed(1.25f) }, label = { Text("1.25×") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                    AssistChip(onClick = { onSpeed(1.5f) }, label = { Text("1.5×") })
                    AssistChip(onClick = { onSpeed(2f) }, label = { Text("2×") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                    AssistChip(onClick = { onSleep(15) }, label = { Text(stringResource(R.string.sleep_15)) })
                    AssistChip(onClick = { onSleep(30) }, label = { Text(stringResource(R.string.sleep_30)) })
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

private fun playbackMinutes(timeMs: Long): Long = timeMs / 60_000L

private fun playbackSeconds(timeMs: Long): Long = (timeMs / 1_000L) % 60L

@Composable
private fun NowPlayingArtwork(nowPlaying: NowPlaying) {
    val fallbackPaletteColor = MaterialTheme.colorScheme.primary
    var bitmap by remember(nowPlaying.track.coverImageUrl) { mutableStateOf<Bitmap?>(null) }
    var paletteColor by remember(nowPlaying.track.coverImageUrl) { mutableStateOf(fallbackPaletteColor) }
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
            paletteColor = withContext(Dispatchers.Default) {
                // Coil may decode artwork as a hardware bitmap. Palette reads pixels directly,
                // so give it a software copy before generating the player gradient.
                val softwareBitmap = cover.copy(Bitmap.Config.ARGB_8888, false)
                Color(Palette.from(softwareBitmap).generate().getVibrantColor(paletteColor.toArgb()))
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(paletteColor, MaterialTheme.colorScheme.surface))),
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
    }
}

@Composable private fun LibraryShell(modifier: Modifier, viewModel: LibraryViewModel = hiltViewModel()) {
 val state by viewModel.state.collectAsState()
 Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
  Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineSmall)
  when (val current = state) {
   LibraryUiState.Loading -> CircularProgressIndicator()
   LibraryUiState.Error -> { Text(stringResource(R.string.library_error)); Button(onClick = viewModel::reload) { Text(stringResource(R.string.retry)) } }
   is LibraryUiState.Content -> {
    Text(stringResource(R.string.liked_count, current.summary.likedCount), style = MaterialTheme.typography.titleMedium)
    if (current.summary.playlists.isEmpty()) Text(stringResource(R.string.library_empty)) else LazyColumn(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) { items(current.summary.playlists, key = { it.id }) { playlist -> Card { Column(Modifier.padding(NavaSpacing.Md)) { Text(playlist.title, style = MaterialTheme.typography.titleMedium); playlist.description?.let { Text(it) } } } } }
   }
  }
 }
}

@Composable
private fun SearchShell(modifier: Modifier, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
        OutlinedTextField(value = state.query, onValueChange = viewModel::updateQuery, label = { Text(stringResource(R.string.search_catalog)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
            AssistChip(onClick = { viewModel.setLanguage(null) }, label = { Text(stringResource(R.string.filter_all)) })
            AssistChip(onClick = { viewModel.setLanguage("en") }, label = { Text(stringResource(R.string.filter_english)) })
            AssistChip(onClick = { viewModel.setLanguage("fa") }, label = { Text(stringResource(R.string.filter_persian)) })
        }
        when {
            state.loading -> CircularProgressIndicator()
            state.failed -> Text(stringResource(R.string.search_error))
            state.query.isNotBlank() && state.results.isEmpty() -> Text(stringResource(R.string.search_empty))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                items(state.results, key = { it.id }) { track -> Card { Column(Modifier.padding(NavaSpacing.Md)) { Text(track.title, style = MaterialTheme.typography.titleMedium); Text(track.artistName, style = MaterialTheme.typography.bodyMedium) } } }
                if (state.canLoadMore) item { LaunchedEffect(state.results.size) { viewModel.loadMore() }; CircularProgressIndicator() }
            }
        }
    }
}

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
        item { Text(stringResource(R.string.home_welcome), modifier = Modifier.padding(horizontal = NavaSpacing.Lg), style = MaterialTheme.typography.headlineSmall) }
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
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
        ) {
            item { QuickAction(R.string.quick_liked) }
            item { QuickAction(R.string.quick_recent) }
            item { QuickAction(R.string.quick_playlists) }
            item { QuickAction(R.string.quick_artists) }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onPlay(track) }, onLongClick = { onQueue(track) }),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(NavaSpacing.Xl), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
            Text(track.title, style = MaterialTheme.typography.titleLarge)
            Text(track.artistName, style = MaterialTheme.typography.bodyMedium)
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
        Text(stringResource(title), modifier = Modifier.padding(horizontal = NavaSpacing.Lg), style = MaterialTheme.typography.titleLarge)
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NavaSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            items(tracks, key = HomeTrack::id) { track ->
                Card(
                    modifier = Modifier.combinedClickable(
                        onClick = { onPlay(track) },
                        onLongClick = { onQueue(track) },
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium)
                        Text(track.artistName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(@StringRes label: Int) {
    AssistChip(onClick = {}, label = { Text(stringResource(label)) })
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
    preferences: UserPreferences,
    onEvent: (NavaEvent) -> Unit,
    modifier: Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::uploadAvatar)
    }
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
            Button(onClick = { avatarPicker.launch("image/*") }, enabled = !state.isSaving) {
                state.avatarUrl?.let { url ->
                    AsyncImage(
                        model = url,
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
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
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
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            text = { Text(error) },
            confirmButton = { Button(onClick = viewModel::dismissError) { Text(stringResource(R.string.close)) } },
        )
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
