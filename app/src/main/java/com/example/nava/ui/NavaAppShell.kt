package com.example.nava.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.nava.R
import com.example.nava.domain.auth.AuthSession
import com.example.nava.domain.catalog.HomeTrack
import com.example.nava.domain.preferences.AppLanguage
import com.example.nava.domain.preferences.ThemeMode
import com.example.nava.domain.preferences.UserPreferences
import com.example.nava.ui.theme.NavaSpacing
import com.example.nava.ui.home.HomeUiState
import com.example.nava.ui.home.HomeViewModel
import com.example.nava.ui.search.SearchViewModel
import com.example.nava.ui.library.LibraryUiState
import com.example.nava.ui.library.LibraryViewModel
import com.example.nava.playback.NowPlaying
import com.example.nava.playback.PlaybackViewModel

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
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val nowPlaying by playbackViewModel.nowPlaying.collectAsState()
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
                nowPlaying?.let { MiniPlayer(it, playbackViewModel::pause, onOpen = { playerExpanded = true }) }
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
            0 -> HomeShell(modifier = Modifier.padding(padding), onPlay = playbackViewModel::play)
            1 -> SearchShell(modifier = Modifier.padding(padding))
            3 -> LibraryShell(modifier = Modifier.padding(padding))
            4 -> ProfileShell(session, preferences, onEvent, Modifier.padding(padding))
            else -> PlaceholderShell(navigationItems[selectedIndex].title, Modifier.padding(padding))
        }
    }
    nowPlaying?.takeIf { playerExpanded }?.let { now ->
        FullPlayer(now, onDismiss = { playerExpanded = false }, onPause = playbackViewModel::pause, onSpeed = playbackViewModel::setSpeed, onSleep = playbackViewModel::setSleepTimer)
    }
}

@Composable
private fun MiniPlayer(nowPlaying: NowPlaying, onPause: () -> Unit, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(nowPlaying.track.title, style = MaterialTheme.typography.titleMedium)
                Text(nowPlaying.track.artistName, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onPause) {
                Icon(if (nowPlaying.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = stringResource(R.string.pause_playback))
            }
        }
    }
}

@Composable
private fun FullPlayer(nowPlaying: NowPlaying, onDismiss: () -> Unit, onPause: () -> Unit, onSpeed: (Float) -> Unit, onSleep: (Long) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(nowPlaying.track.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
                Text(nowPlaying.track.artistName, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                    AssistChip(onClick = { onSpeed(0.75f) }, label = { Text("0.75×") })
                    AssistChip(onClick = { onSpeed(1f) }, label = { Text("1×") })
                    AssistChip(onClick = { onSpeed(1.25f) }, label = { Text("1.25×") })
                    AssistChip(onClick = { onSpeed(1.5f) }, label = { Text("1.5×") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                    AssistChip(onClick = { onSleep(15) }, label = { Text(stringResource(R.string.sleep_15)) })
                    AssistChip(onClick = { onSleep(30) }, label = { Text(stringResource(R.string.sleep_30)) })
                }
            }
        },
        confirmButton = { Button(onClick = onPause) { Text(stringResource(R.string.pause_playback)) } },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
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
) {
    val state by viewModel.uiState.collectAsState()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xl),
    ) {
        item { Text(stringResource(R.string.home_welcome), modifier = Modifier.padding(horizontal = NavaSpacing.Lg), style = MaterialTheme.typography.headlineSmall) }
        when (val current = state) {
            HomeUiState.Loading -> item { HomeLoading() }
            HomeUiState.Error -> item { HomeError(onRetry = viewModel::reload) }
            is HomeUiState.Content -> homeContent(current, onPlay)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeContent(state: HomeUiState.Content, onPlay: (HomeTrack) -> Unit) {
    item {
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NavaSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            items(state.feed.featured, key = HomeTrack::id) { FeaturedCard(it, onPlay) }
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
    item { DiscoverySection(R.string.home_trending, state.feed.trending, onPlay) }
    item { DiscoverySection(R.string.home_newest, state.feed.newest, onPlay) }
    item { DiscoverySection(R.string.home_global, state.feed.global, onPlay) }
    item { DiscoverySection(R.string.home_local, state.feed.local, onPlay) }
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
private fun FeaturedCard(track: HomeTrack, onPlay: (HomeTrack) -> Unit) {
    Card(
        onClick = { onPlay(track) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(NavaSpacing.Xl), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
            Text(track.title, style = MaterialTheme.typography.titleLarge)
            Text(track.artistName, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DiscoverySection(@StringRes title: Int, tracks: List<HomeTrack>, onPlay: (HomeTrack) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
        Text(stringResource(title), modifier = Modifier.padding(horizontal = NavaSpacing.Lg), style = MaterialTheme.typography.titleLarge)
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NavaSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            items(tracks, key = HomeTrack::id) { track ->
                Card(onClick = { onPlay(track) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
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
) {
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = stringResource(R.string.user_avatar), modifier = Modifier.size(NavaSpacing.Xxl))
            Column {
                Text(session.email.substringBefore('@'), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.standard), style = MaterialTheme.typography.bodyMedium)
            }
        }
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
