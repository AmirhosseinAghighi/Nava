package com.example.nava.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.dp
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
import com.example.nava.ui.home.HomeQuickViewModel
import com.example.nava.ui.home.HomeViewModel
import com.example.nava.ui.search.SearchViewModel
import com.example.nava.ui.search.SearchArtistResult
import com.example.nava.ui.search.SearchGenreResult
import com.example.nava.ui.search.SearchResultFilter
import com.example.nava.ui.library.LibraryUiState
import com.example.nava.ui.library.LibraryViewModel
import com.example.nava.ui.library.LikesViewModel
import com.example.nava.domain.library.PlaylistTrack
import com.example.nava.domain.library.UserPlaylist
import com.example.nava.ui.downloads.DownloadViewModel
import com.example.nava.ui.downloads.DownloadUiError
import com.example.nava.ui.downloads.DownloadsUiState
import com.example.nava.data.downloads.OfflineTrackEntity
import com.example.nava.ui.profile.ProfileViewModel
import com.example.nava.ui.profile.ProfileUiState
import com.example.nava.ui.social.SocialViewModel
import com.example.nava.ui.social.PublicPlaylist
import com.example.nava.ui.social.SocialProfileDetails
import com.example.nava.ui.social.SocialPerson
import com.example.nava.ui.social.SocialSection
import com.example.nava.ui.chat.ChatShell
import com.example.nava.ui.chat.ChatConversation
import com.example.nava.ui.chat.ChatViewModel
import com.example.nava.playback.NowPlaying
import com.example.nava.playback.PlaybackViewModel
import com.example.nava.playback.RepeatMode
import com.example.nava.ui.theme.NavaMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

private const val SHARE_SUCCESS_VISIBLE_MS = 2_500L

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
private fun TopBarProfileAvatar(
    state: ProfileUiState,
    onClick: () -> Unit,
) {
    val avatarModel = state.pendingAvatarUri ?: state.avatarUrl
    Box(
        modifier = Modifier
            .padding(start = NavaSpacing.Sm)
            .size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = NavaSpacing.Xs,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    avatarModel != null -> UserAvatar(
                        model = avatarModel,
                        contentDescription = stringResource(R.string.profile),
                        modifier = Modifier.fillMaxSize(),
                    )
                    state.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    else -> Icon(
                        Icons.Outlined.AccountCircle,
                        contentDescription = stringResource(R.string.profile),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (state.isPremium) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.premium_member),
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationBell(
    unreadCount: Long,
    onClick: () -> Unit,
) {
    val notificationDescription = if (unreadCount > 0) {
        stringResource(R.string.unread_notifications, unreadCount)
    } else {
        stringResource(R.string.notification)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = notificationDescription },
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = null)
        }
        if (unreadCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .height(18.dp)
                    .widthIn(min = 18.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                shadowElevation = 2.dp,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
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
    var notificationsOpen by rememberSaveable { mutableStateOf(false) }
    var notificationChatOpen by rememberSaveable { mutableStateOf(false) }
    var socialInitialSection by rememberSaveable { mutableStateOf(SocialSection.PEOPLE) }
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var queueCandidate by remember { mutableStateOf<HomeTrack?>(null) }
    var shareCandidate by remember { mutableStateOf<HomeTrack?>(null) }
    var shareSuccessRecipient by remember { mutableStateOf<String?>(null) }
    var addToPlaylistCandidate by remember { mutableStateOf<HomeTrack?>(null) }
    val playbackViewModel: PlaybackViewModel = hiltViewModel(key = "playback:${session.userId}")
    val downloadViewModel: DownloadViewModel = hiltViewModel(key = "downloads:${session.userId}")
    val likesViewModel: LikesViewModel = hiltViewModel(key = "likes:${session.userId}")
    val chatViewModel: ChatViewModel = hiltViewModel(key = "chat:${session.userId}")
    val profileViewModel: ProfileViewModel = hiltViewModel(key = "profile:${session.userId}")
    val socialViewModel: SocialViewModel = hiltViewModel(key = "social:${session.userId}")
    val libraryViewModel: LibraryViewModel = hiltViewModel(key = "library:${session.userId}")
    val searchViewModel: SearchViewModel = hiltViewModel(key = "search:${session.userId}")
    val nowPlaying by playbackViewModel.nowPlaying.collectAsState()
    val playbackSpeed by playbackViewModel.playbackSpeed.collectAsState()
    val sleepTimerMinutes by playbackViewModel.sleepTimerMinutes.collectAsState()
    val shuffleEnabled by playbackViewModel.shuffleEnabled.collectAsState()
    val repeatMode by playbackViewModel.repeatMode.collectAsState()
    val fftBands by playbackViewModel.fftBands.collectAsState()
    val playbackError by playbackViewModel.playbackError.collectAsState()
    val downloadState by downloadViewModel.state.collectAsState()
    val downloadError by downloadViewModel.downloadError.collectAsState()
    val likesState by likesViewModel.state.collectAsState()
    val chatState by chatViewModel.state.collectAsState()
    val libraryState by libraryViewModel.state.collectAsState()
    val profileState by profileViewModel.state.collectAsState()
    val unreadConversations = chatState.conversations.filter { it.unreadCount > 0 }
    val unreadMessageCount = unreadConversations.sumOf(ChatConversation::unreadCount)
    LaunchedEffect(shareSuccessRecipient) {
        if (shareSuccessRecipient != null) {
            delay(SHARE_SUCCESS_VISIBLE_MS)
            shareSuccessRecipient = null
        }
    }
    BackHandler(enabled = settingsOpen) {
        settingsOpen = false
    }
    BackHandler(enabled = notificationsOpen && !notificationChatOpen) {
        notificationsOpen = false
    }
    BackHandler(enabled = notificationChatOpen) {
        chatViewModel.closeConversation()
        notificationChatOpen = false
    }
    BackHandler(
        enabled = !settingsOpen &&
            !socialOpen &&
            !notificationsOpen &&
            !playerExpanded &&
            queueCandidate == null &&
            shareCandidate == null &&
            addToPlaylistCandidate == null &&
            selectedIndex != 0,
    ) {
        selectedIndex = 0
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (settingsOpen || socialOpen || notificationsOpen) {
                        IconButton(onClick = {
                            if (notificationChatOpen) {
                                chatViewModel.closeConversation()
                                notificationChatOpen = false
                            } else {
                                settingsOpen = false
                                socialOpen = false
                                notificationsOpen = false
                            }
                        }) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                title = {
                    when {
                        settingsOpen -> Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
                        socialOpen -> Text(stringResource(R.string.discover_people), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        notificationsOpen -> Text(stringResource(R.string.notification), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        else -> NavaTopBarBrand()
                    }
                },
                actions = {
                    if (!settingsOpen && !socialOpen && !notificationsOpen) {
                        NotificationBell(
                            unreadCount = unreadMessageCount,
                            onClick = {
                                chatViewModel.refreshInbox()
                                notificationsOpen = true
                                notificationChatOpen = false
                            },
                        )
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.open_settings))
                        }
                        TopBarProfileAvatar(
                            state = profileState,
                            onClick = {
                                selectedIndex = 4
                                settingsOpen = false
                                socialOpen = false
                            },
                        )
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
                            onClick = {
                                selectedIndex = index
                                settingsOpen = false
                                socialOpen = false
                                notificationsOpen = false
                                notificationChatOpen = false
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.title)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            settingsOpen -> SettingsShell(session, preferences, onEvent, Modifier.padding(padding))
            notificationsOpen && notificationChatOpen -> ChatShell(
                modifier = Modifier.padding(padding),
                onBack = { notificationChatOpen = false },
                onConversationBack = { notificationChatOpen = false },
                viewModel = chatViewModel,
                playbackViewModel = playbackViewModel,
            )
            notificationsOpen -> ChatNotificationsScreen(
                modifier = Modifier.padding(padding),
                conversations = unreadConversations,
                loading = chatState.loading,
                error = chatState.error,
                onRetry = chatViewModel::refreshInbox,
                onOpenConversation = { conversation ->
                    chatViewModel.openConversation(conversation)
                    notificationChatOpen = true
                },
            )
            socialOpen -> SocialShell(
                modifier = Modifier.padding(padding),
                onBack = { socialOpen = false },
                initialSection = socialInitialSection,
                viewModel = socialViewModel,
                chatViewModel = chatViewModel,
                playbackViewModel = playbackViewModel,
            )
            selectedIndex == 0 -> HomeShell(
                modifier = Modifier.padding(padding),
                likedTracks = likesState.songs,
                currentTrackId = nowPlaying?.track?.id,
                onPlay = { track ->
                    if (nowPlaying?.track?.id != track.id) playbackViewModel.play(track)
                    playerExpanded = true
                },
                onQueue = { queueCandidate = it },
                onTrackOptions = { queueCandidate = it },
                onOpenMyPlaylists = { selectedIndex = 3 },
                onOpenTopPlaylists = {
                    socialInitialSection = SocialSection.PLAYLISTS
                    socialOpen = true
                },
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
                viewModel = searchViewModel,
            )
            selectedIndex == 2 -> DownloadsShell(
                modifier = Modifier.padding(padding),
                state = downloadState,
                currentTrackId = nowPlaying?.track?.id,
                viewModel = downloadViewModel,
                onPlay = { download ->
                    playbackViewModel.playOffline(download)
                    playerExpanded = true
                },
            )
            selectedIndex == 3 -> LibraryShell(
                modifier = Modifier.padding(padding),
                likesViewModel = likesViewModel,
                currentTrackId = nowPlaying?.track?.id,
                onTrackClick = { track ->
                    if (nowPlaying?.track?.id != track.id) playbackViewModel.play(track)
                    playerExpanded = true
                },
                onTrackOptions = { queueCandidate = it },
                viewModel = libraryViewModel,
            )
            selectedIndex == 4 -> ProfileShell(
                session = session,
                modifier = Modifier.padding(padding),
                onOpenSocial = { section ->
                    socialInitialSection = section
                    socialOpen = true
                },
                viewModel = profileViewModel,
            )
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
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                isLiked = now.track.id in likesState.likedIds,
                isDownloaded = now.track.id in downloadState.downloadedTrackIds,
                isDownloading = now.track.id in downloadState.downloadingTrackIds,
                downloadProgressPercent = downloadState.activeDownloads
                    .firstOrNull { it.trackId == now.track.id }
                    ?.progressPercent,
                fftBands = fftBands,
                onDismiss = { playerExpanded = false },
                onToggle = { if (now.playing) playbackViewModel.pause() else playbackViewModel.resume() },
                onSeek = playbackViewModel::seekTo,
                onCycleSpeed = playbackViewModel::cycleSpeed,
                onCycleSleepTimer = playbackViewModel::cycleSleepTimer,
                onToggleShuffle = playbackViewModel::toggleShuffle,
                onCycleRepeat = playbackViewModel::cycleRepeatMode,
                onToggleLike = { likesViewModel.toggle(now.track) },
                onDownload = { downloadViewModel.request(now.track) },
                onAddToPlaylist = {
                    libraryViewModel.clearOperationError()
                    addToPlaylistCandidate = now.track
                },
                onShare = { shareCandidate = now.track },
                onPrevious = playbackViewModel::skipToPrevious,
                onNext = playbackViewModel::skipToNext,
            )
        }
    }
    addToPlaylistCandidate?.let { track ->
        AddToPlaylistSheet(
            track = track,
            playlists = libraryState.summary.playlists,
            loading = libraryState.loading,
            busy = libraryState.busy,
            operationFailed = libraryState.operationFailed,
            onDismiss = { addToPlaylistCandidate = null },
            onRetry = {
                libraryViewModel.clearOperationError()
                libraryViewModel.reload()
            },
            onPlaylistSelected = { playlist ->
                libraryViewModel.addTrackToPlaylist(playlist.id, track.id) { success ->
                    if (success) addToPlaylistCandidate = null
                }
            },
        )
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
        val isLiked = track.id in likesState.likedIds
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
                SongActionRow(
                    icon = Icons.Outlined.Share,
                    label = stringResource(R.string.share_track),
                    onClick = {
                        shareCandidate = track
                        queueCandidate = null
                    },
                )
                Spacer(Modifier.height(NavaSpacing.Lg))
            }
        }
    }
    shareCandidate?.let { track ->
        TrackShareSheet(
            track = track,
            conversations = chatState.conversations,
            loading = chatState.loading,
            sending = chatState.sending,
            onDismiss = { shareCandidate = null },
            onShare = { conversation ->
                chatViewModel.shareTrack(conversation, track) {
                    shareCandidate = null
                    shareSuccessRecipient = conversation.peerName
                }
            },
            onOpenPeople = {
                shareCandidate = null
                playerExpanded = false
                socialInitialSection = SocialSection.PEOPLE
                socialOpen = true
            },
        )
    }
    downloadError?.let { error ->
        AlertDialog(
            onDismissRequest = downloadViewModel::dismissDownloadError,
            title = {
                Text(
                    text = stringResource(
                        if (error == DownloadUiError.PremiumRequired) R.string.premium_required_title
                        else R.string.download_unavailable_title,
                    ),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    stringResource(
                        if (error == DownloadUiError.PremiumRequired) R.string.premium_download_required
                        else R.string.download_unavailable,
                    ),
                )
            },
            confirmButton = {
                Button(onClick = downloadViewModel::dismissDownloadError) {
                    Text(stringResource(R.string.got_it))
                }
            },
        )
    }
    AnimatedVisibility(
        visible = shareSuccessRecipient != null,
        enter = fadeIn(tween(NavaMotion.Fast)) + slideInVertically(tween(NavaMotion.Standard)) { -it },
        exit = fadeOut(tween(NavaMotion.Fast)) + slideOutVertically(tween(NavaMotion.Standard)) { -it },
    ) {
        ShareSuccessPopup(recipientName = shareSuccessRecipient.orEmpty())
    }
}

@Composable
private fun ShareSuccessPopup(recipientName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = NavaSpacing.Lg)
            .padding(top = 72.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = NavaSpacing.Md,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = NavaSpacing.Lg, vertical = NavaSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(5.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.track_shared_success, recipientName),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
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
@OptIn(ExperimentalMaterial3Api::class)
private fun TrackShareSheet(
    track: HomeTrack,
    conversations: List<ChatConversation>,
    loading: Boolean,
    sending: Boolean,
    onDismiss: () -> Unit,
    onShare: (ChatConversation) -> Unit,
    onOpenPeople: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = NavaSpacing.Lg, vertical = NavaSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Text(stringResource(R.string.share_track), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large)
                    .padding(NavaSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
            ) {
                AsyncImage(
                    model = track.coverImageUrl,
                    contentDescription = stringResource(R.string.track_artwork, track.title),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    modifier = Modifier.size(NavaDimensions.SearchActionArtworkSize).clip(MaterialTheme.shapes.medium),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(track.artistName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Text(stringResource(R.string.choose_share_recipient), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(NavaSpacing.Xl), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                conversations.isEmpty() -> {
                    Text(stringResource(R.string.share_no_conversations), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(NavaDimensions.ShareRecipientListHeight),
                    verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                ) {
                    items(conversations, key = ChatConversation::id) { conversation ->
                        Surface(
                            onClick = { onShare(conversation) },
                            enabled = !sending,
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
                                    modifier = Modifier.size(NavaDimensions.PlayerModeControlSize),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(conversation.peerName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(conversation.peerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.tap_to_share), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_with, conversation.peerName))
                            }
                        }
                    }
                }
            }
            FilledTonalButton(onClick = onOpenPeople, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Groups, contentDescription = null)
                Spacer(Modifier.size(NavaSpacing.Sm))
                Text(stringResource(R.string.discover_people))
            }
            Spacer(Modifier.height(NavaSpacing.Sm))
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
@OptIn(ExperimentalMaterial3Api::class)
private fun AddToPlaylistSheet(
    track: HomeTrack,
    playlists: List<UserPlaylist>,
    loading: Boolean,
    busy: Boolean,
    operationFailed: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onPlaylistSelected: (UserPlaylist) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = NavaSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Text(
                text = stringResource(R.string.choose_playlist),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.choose_playlist_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                ) {
                    AsyncImage(
                        model = track.coverImageUrl,
                        contentDescription = stringResource(R.string.track_artwork, track.title),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_foreground),
                        modifier = Modifier.size(58.dp).clip(MaterialTheme.shapes.medium),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            track.artistName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                        )
                    }
                }
            }
            when {
                loading || busy -> Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                operationFailed -> Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                    ) {
                        Text(stringResource(R.string.playlist_action_failed), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.playlist_action_failed_hint),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
                playlists.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = NavaSpacing.Xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.PlaylistAdd, contentDescription = null, modifier = Modifier.size(34.dp))
                        }
                    }
                    Text(stringResource(R.string.no_playlists_for_track), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.no_playlists_for_track_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                ) {
                    items(playlists, key = UserPlaylist::id) { playlist ->
                        Surface(
                            onClick = { onPlaylistSelected(playlist) },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                            ) {
                                PlaylistArtwork(playlist.coverImageUrl, playlist.title, Modifier.size(62.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        playlist.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        stringResource(
                                            if (playlist.isPublic) R.string.public_playlist_summary else R.string.private_playlist_summary,
                                            playlist.trackCount.toLong(),
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_to_playlist))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(NavaSpacing.Sm))
        }
    }
}

@Composable
private fun FullPlayer(
    nowPlaying: NowPlaying,
    playbackSpeed: Float,
    sleepTimerMinutes: Long?,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgressPercent: Int?,
    fftBands: FloatArray,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
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
                PlayerHeader(
                    speedLabel = speedLabel,
                    sleepTimerMinutes = sleepTimerMinutes,
                    onDismiss = onDismiss,
                    onCycleSpeed = onCycleSpeed,
                    onCycleSleepTimer = onCycleSleepTimer,
                    onShare = onShare,
                )
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
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onPrevious = onPrevious,
                        onToggle = onToggle,
                        onNext = onNext,
                    )
                    PlayerLibraryActions(
                        isLiked = isLiked,
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        downloadProgressPercent = downloadProgressPercent,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        onDownload = onDownload,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    speedLabel: String,
    sleepTimerMinutes: Long?,
    onDismiss: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onShare: () -> Unit,
) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
            PlayerTopActionButton(
                contentDescription = stringResource(R.string.playback_speed_value, speedLabel),
                onClick = onCycleSpeed,
            ) {
                Text("${speedLabel}×", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            PlayerTopActionButton(
                contentDescription = sleepTimerMinutes?.let { stringResource(R.string.sleep_timer_minutes, it) }
                    ?: stringResource(R.string.sleep_timer_off),
                selected = sleepTimerMinutes != null,
                onClick = onCycleSleepTimer,
            ) {
                if (sleepTimerMinutes == null) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(NavaSpacing.Xl))
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(
                            stringResource(R.string.sleep_timer_compact, sleepTimerMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            PlayerTopActionButton(
                contentDescription = stringResource(R.string.share_track),
                onClick = onShare,
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(NavaSpacing.Xl))
            }
        }
    }
}

@Composable
private fun PlayerTopActionButton(
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .82f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun PlayerTransportControls(
    playing: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerModeButton(
            icon = Icons.Outlined.Shuffle,
            contentDescription = if (shuffleEnabled) R.string.shuffle_enabled else R.string.shuffle,
            selected = shuffleEnabled,
            onClick = onToggleShuffle,
        )
        PlayerControlButton(Icons.Outlined.SkipPrevious, R.string.previous_track, onPrevious)
        PlayerControlButton(
            icon = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = if (playing) R.string.pause_playback else R.string.resume_playback,
            onClick = onToggle,
            primary = true,
        )
        PlayerControlButton(Icons.Outlined.SkipNext, R.string.next_track, onNext)
        PlayerModeButton(
            icon = if (repeatMode == RepeatMode.One) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
            contentDescription = when (repeatMode) {
                RepeatMode.Off -> R.string.repeat_off
                RepeatMode.All -> R.string.repeat_all
                RepeatMode.One -> R.string.repeat_one
            },
            selected = repeatMode != RepeatMode.Off,
            onClick = onCycleRepeat,
        )
    }
}

@Composable
private fun PlayerModeButton(
    icon: ImageVector,
    @StringRes contentDescription: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(NavaDimensions.PlayerModeControlSize),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = stringResource(contentDescription),
                modifier = Modifier.size(NavaSpacing.Xl),
            )
        }
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
private fun PlayerLibraryActions(
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgressPercent: Int?,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
) {
    val panelShape = MaterialTheme.shapes.extraLarge
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
                shape = panelShape,
            ),
        shape = panelShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .58f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = NavaSpacing.Sm, vertical = NavaSpacing.Sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            PlayerRoundActionButton(
                icon = if (isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                label = stringResource(if (isLiked) R.string.liked else R.string.like_song),
                onClick = onToggleLike,
                selected = isLiked,
                modifier = Modifier.weight(1f),
            )
            PlayerRoundActionButton(
                icon = Icons.Outlined.PlaylistAdd,
                label = stringResource(R.string.add_to_playlist),
                onClick = onAddToPlaylist,
                primary = true,
                modifier = Modifier.weight(1f),
            )
            PlayerDownloadAction(
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                progressPercent = downloadProgressPercent,
                onClick = onDownload,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlayerRoundActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    primary: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs),
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(if (primary) 48.dp else 44.dp),
            shape = CircleShape,
            color = when {
                primary -> MaterialTheme.colorScheme.primary
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = when {
                primary -> MaterialTheme.colorScheme.onPrimary
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(if (primary) 24.dp else 22.dp))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun PlayerDownloadAction(
    isDownloaded: Boolean,
    isDownloading: Boolean,
    progressPercent: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetProgress = when {
        isDownloaded -> 1f
        isDownloading -> ((progressPercent ?: 0) / 100f).coerceIn(.025f, 1f)
        else -> 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(NavaMotion.Fast),
        label = "downloadRingProgress",
    )
    val ringTrackColor = MaterialTheme.colorScheme.outlineVariant
    val ringProgressColor = MaterialTheme.colorScheme.primary
    val label = when {
        isDownloaded -> stringResource(R.string.downloaded)
        isDownloading -> stringResource(R.string.download_progress, progressPercent ?: 0)
        else -> stringResource(R.string.download)
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs),
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.dp.toPx()
                drawCircle(
                    color = ringTrackColor,
                    style = Stroke(width = strokeWidth),
                    radius = size.minDimension / 2f - strokeWidth,
                )
                if (animatedProgress > 0f) {
                    drawArc(
                        color = ringProgressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }
            Surface(
                onClick = onClick,
                enabled = !isDownloaded && !isDownloading,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (isDownloaded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isDownloaded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isDownloaded) Icons.Outlined.DownloadDone else Icons.Outlined.Download,
                        contentDescription = label,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
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

@Composable
private fun LibraryShell(
    modifier: Modifier,
    likesViewModel: LikesViewModel,
    currentTrackId: String?,
    onTrackClick: (HomeTrack) -> Unit,
    onTrackOptions: (HomeTrack) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val likes by likesViewModel.state.collectAsState()
    var editorOpen by remember { mutableStateOf(false) }
    var editorPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var deleteCandidate by remember { mutableStateOf<UserPlaylist?>(null) }
    var trackActionCandidate by remember { mutableStateOf<PlaylistTrack?>(null) }
    var trackPickerOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = state.selectedPlaylist != null) { viewModel.closePlaylist() }

    val selectedPlaylist = state.selectedPlaylist
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.failed -> SearchMessage(
                title = stringResource(R.string.library_error),
                body = stringResource(R.string.library_error_hint),
                action = { Button(onClick = viewModel::reload) { Text(stringResource(R.string.retry)) } },
            )
            selectedPlaylist != null -> PlaylistDetailsScreen(
                details = selectedPlaylist,
                currentTrackId = currentTrackId,
                busy = state.busy || state.loadingDetails,
                onBack = viewModel::closePlaylist,
                onEdit = {
                    editorPlaylist = selectedPlaylist.playlist
                    editorOpen = true
                },
                onAddTracks = {
                    trackPickerOpen = true
                    viewModel.loadCatalog()
                },
                onTrackClick = { onTrackClick(it.toHomeTrack()) },
                onTrackOptions = { trackActionCandidate = it },
            )
            else -> PlaylistOverviewScreen(
                playlists = state.summary.playlists,
                likedCount = likes.songs.size,
                onCreate = {
                    editorPlaylist = null
                    editorOpen = true
                },
                onOpen = viewModel::openPlaylist,
                onEdit = {
                    editorPlaylist = it
                    editorOpen = true
                },
            )
        }
        if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
    }

    if (editorOpen) {
        PlaylistEditorDialog(
            playlist = editorPlaylist,
            busy = state.busy,
            onDismiss = { editorOpen = false },
            onSave = { title, description, isPublic, coverUri ->
                editorPlaylist?.let { viewModel.updatePlaylist(it.id, title, description, isPublic, coverUri) }
                    ?: viewModel.createPlaylist(title, description, isPublic, coverUri)
                editorOpen = false
            },
            onDelete = editorPlaylist?.let { playlist ->
                {
                    editorOpen = false
                    deleteCandidate = playlist
                }
            },
        )
    }

    deleteCandidate?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            icon = {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(NavaSpacing.Md),
                    )
                }
            },
            title = { Text(stringResource(R.string.delete_playlist), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                    Text(stringResource(R.string.delete_playlist_confirmation, playlist.title))
                    Text(
                        stringResource(R.string.delete_playlist_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(NavaSpacing.Xs))
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteCandidate = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    trackActionCandidate?.let { track ->
        PlaylistTrackActionsDialog(
            track = track,
            onDismiss = { trackActionCandidate = null },
            onPlay = {
                onTrackClick(track.toHomeTrack())
                trackActionCandidate = null
            },
            onMore = {
                onTrackOptions(track.toHomeTrack())
                trackActionCandidate = null
            },
            onRemove = {
                viewModel.removeTrack(track.id)
                trackActionCandidate = null
            },
        )
    }

    if (trackPickerOpen) {
        PlaylistTrackPicker(
            catalog = state.catalog,
            existingTrackIds = state.selectedPlaylist?.tracks.orEmpty().mapTo(mutableSetOf(), PlaylistTrack::id),
            loading = state.busy && state.catalog.isEmpty(),
            onDismiss = { trackPickerOpen = false },
            onAdd = viewModel::addTrack,
        )
    }

    if (state.operationFailed) {
        AlertDialog(
            onDismissRequest = viewModel::clearOperationError,
            title = { Text(stringResource(R.string.playlist_action_failed)) },
            text = { Text(stringResource(R.string.playlist_action_failed_hint)) },
            confirmButton = {
                Button(onClick = viewModel::clearOperationError) { Text(stringResource(R.string.close)) }
            },
        )
    }
}

@Composable
private fun PlaylistOverviewScreen(
    playlists: List<UserPlaylist>,
    likedCount: Int,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onEdit: (UserPlaylist) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = NavaSpacing.Lg,
            end = NavaSpacing.Lg,
            top = NavaSpacing.Lg,
            bottom = NavaSpacing.Xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.playlist_library_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onCreate) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(NavaSpacing.Sm))
                    Text(stringResource(R.string.new_playlist))
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) {
                        Icon(
                            Icons.Outlined.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(NavaSpacing.Md),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.liked_songs), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.track_count, likedCount.toLong()), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.your_playlists), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (playlists.isEmpty()) {
            item { EmptyPlaylistsCard(onCreate) }
        } else {
            items(playlists, key = UserPlaylist::id) { playlist ->
                UserPlaylistCard(playlist, onOpen = { onOpen(playlist.id) }, onEdit = { onEdit(playlist) })
            }
        }
    }
}

@Composable
private fun EmptyPlaylistsCard(onCreate: () -> Unit) {
    Card(
        onClick = onCreate,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(72.dp)) {
                Icon(Icons.Outlined.PlaylistAdd, contentDescription = null, modifier = Modifier.padding(NavaSpacing.Lg))
            }
            Text(stringResource(R.string.no_playlists_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.no_playlists_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun UserPlaylistCard(playlist: UserPlaylist, onOpen: () -> Unit, onEdit: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            PlaylistArtwork(playlist.coverImageUrl, playlist.title, Modifier.size(82.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(playlist.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                playlist.description?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (playlist.isPublic) Icons.Outlined.Public else Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(stringResource(R.string.track_count, playlist.trackCount.toLong()), style = MaterialTheme.typography.labelLarge)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_playlist)) }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun PlaylistDetailsScreen(
    details: com.example.nava.domain.library.PlaylistDetails,
    currentTrackId: String?,
    busy: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddTracks: () -> Unit,
    onTrackClick: (PlaylistTrack) -> Unit,
    onTrackOptions: (PlaylistTrack) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = NavaSpacing.Lg,
            end = NavaSpacing.Lg,
            top = NavaSpacing.Md,
            bottom = NavaSpacing.Xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) }
                Text(stringResource(R.string.playlist_details), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_playlist)) }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                ) {
                    PlaylistArtwork(details.playlist.coverImageUrl, details.playlist.title, Modifier.size(156.dp))
                    Text(details.playlist.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    details.playlist.description?.let {
                        Text(it, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
                    }
                    Text(
                        stringResource(
                            if (details.playlist.isPublic) R.string.public_playlist_summary else R.string.private_playlist_summary,
                            details.tracks.size.toLong(),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Button(onClick = onAddTracks, enabled = !busy) {
                        Icon(Icons.Outlined.PlaylistAdd, contentDescription = null)
                        Spacer(Modifier.width(NavaSpacing.Sm))
                        Text(stringResource(R.string.add_songs))
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.playlist_songs), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (details.tracks.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                    ) {
                        Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.playlist_no_songs), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.playlist_no_songs_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(details.tracks, key = PlaylistTrack::id) { track ->
                PlaylistTrackRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    onClick = { onTrackClick(track) },
                    onOptions = { onTrackOptions(track) },
                )
            }
        }
    }
}

@Composable
private fun PlaylistArtwork(url: String?, title: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
        if (url.isNullOrBlank()) {
            Icon(Icons.Outlined.QueueMusic, contentDescription = null, modifier = Modifier.padding(NavaSpacing.Lg), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = stringResource(R.string.playlist_cover, title),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PlaylistTrackRow(track: PlaylistTrack, isCurrent: Boolean, onClick: () -> Unit, onOptions: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(track.coverImageUrl).crossfade(true).build(),
                    contentDescription = stringResource(R.string.track_artwork, track.title),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    modifier = Modifier.size(NavaDimensions.SearchTrackArtworkSize).clip(MaterialTheme.shapes.medium),
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(NavaDimensions.SearchPlayBadgeSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.padding(NavaSpacing.Xs), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artistName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatTrackDuration(track.durationSeconds), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onOptions) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.track_more_options, track.title)) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlaylistEditorDialog(
    playlist: UserPlaylist?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String?, Boolean, Uri?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(playlist?.id) { mutableStateOf(playlist?.title.orEmpty()) }
    var description by remember(playlist?.id) { mutableStateOf(playlist?.description.orEmpty()) }
    var isPublic by remember(playlist?.id) { mutableStateOf(playlist?.isPublic ?: false) }
    var selectedCover by remember(playlist?.id) { mutableStateOf<Uri?>(null) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedCover = uri
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NavaSpacing.Lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        if (playlist == null) Icons.Outlined.PlaylistAdd else Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.padding(NavaSpacing.Sm),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(NavaSpacing.Md))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (playlist == null) R.string.create_playlist else R.string.edit_playlist),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.playlist_editor_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.close)) }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
            ) {
                Box {
                    PlaylistEditorArtwork(
                        model = selectedCover ?: playlist?.coverImageUrl,
                        title = title.ifBlank { stringResource(R.string.playlist_details) },
                        modifier = Modifier.size(140.dp),
                    )
                    Surface(
                        onClick = { coverPicker.launch("image/*") },
                        modifier = Modifier.align(Alignment.BottomEnd).size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp,
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.change_playlist_cover),
                            modifier = Modifier.padding(NavaSpacing.Sm),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                FilledTonalButton(onClick = { coverPicker.launch("image/*") }) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.width(NavaSpacing.Xs))
                    Text(stringResource(if (selectedCover == null && playlist?.coverImageUrl.isNullOrBlank()) R.string.choose_playlist_cover else R.string.change_playlist_cover))
                }
                Text(
                    stringResource(R.string.playlist_cover_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                supportingText = { Text("${title.length}/120") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.playlist_description)) },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(
                            if (isPublic) Icons.Outlined.Public else Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(NavaSpacing.Sm),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.public_playlist), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.public_playlist_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }

            Button(
                enabled = title.isNotBlank() && !busy,
                onClick = { onSave(title.trim(), description.trim().takeIf(String::isNotEmpty), isPublic, selectedCover) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(Modifier.width(NavaSpacing.Sm))
                Text(stringResource(R.string.save_changes))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                onDelete?.let {
                    OutlinedButton(
                        onClick = it,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(NavaSpacing.Xs))
                        Text(stringResource(R.string.delete_playlist_action))
                    }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            }
            Spacer(Modifier.height(NavaSpacing.Md))
        }
    }
}

@Composable
private fun PlaylistEditorArtwork(model: Any?, title: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 8.dp,
    ) {
        if (model == null) {
            Icon(
                Icons.Outlined.QueueMusic,
                contentDescription = null,
                modifier = Modifier.padding(NavaSpacing.Xl),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(model).crossfade(true).build(),
                contentDescription = stringResource(R.string.playlist_cover, title),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PlaylistTrackActionsDialog(
    track: PlaylistTrack,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    onRemove: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(track.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                SongActionRow(Icons.Outlined.PlayArrow, stringResource(R.string.play_now), onClick = onPlay)
                SongActionRow(Icons.Outlined.MoreVert, stringResource(R.string.more_song_actions), onClick = onMore)
                SongActionRow(Icons.Outlined.Delete, stringResource(R.string.remove_from_playlist), onClick = onRemove)
            }
        },
        confirmButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistTrackPicker(
    catalog: List<PlaylistTrack>,
    existingTrackIds: Set<String>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(catalog, query) {
        catalog.filter { query.isBlank() || it.title.contains(query, true) || it.artistName.contains(query, true) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = NavaSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Text(stringResource(R.string.add_songs), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_playlist_songs)) },
                leadingIcon = { Icon(Icons.Outlined.ManageSearch, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            when {
                loading -> Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                filtered.isEmpty() -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_tracks_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = NavaSpacing.Xl),
                ) {
                    items(filtered, key = PlaylistTrack::id) { track ->
                        val added = track.id in existingTrackIds
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                            ) {
                                AsyncImage(
                                    model = track.coverImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artistName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                FilledTonalButton(onClick = { onAdd(track.id) }, enabled = !added) {
                                    Icon(if (added) Icons.Outlined.Check else Icons.Outlined.Add, contentDescription = null)
                                    Spacer(Modifier.width(NavaSpacing.Xs))
                                    Text(stringResource(if (added) R.string.added else R.string.add))
                                }
                            }
                        }
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
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
            }
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
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.submitSearch()
                    focusManager.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(
                    stringResource(R.string.filter_result_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                    item {
                        SearchFilterChip(
                            selected = state.resultFilter == SearchResultFilter.All,
                            label = R.string.filter_all,
                            icon = Icons.Outlined.ManageSearch,
                            onClick = { viewModel.setResultFilter(SearchResultFilter.All) },
                        )
                    }
                    item {
                        SearchFilterChip(
                            selected = state.resultFilter == SearchResultFilter.Tracks,
                            label = R.string.filter_tracks,
                            icon = Icons.Outlined.MusicNote,
                            onClick = { viewModel.setResultFilter(SearchResultFilter.Tracks) },
                        )
                    }
                    item {
                        SearchFilterChip(
                            selected = state.resultFilter == SearchResultFilter.Artists,
                            label = R.string.filter_artists,
                            icon = Icons.Outlined.AccountCircle,
                            onClick = { viewModel.setResultFilter(SearchResultFilter.Artists) },
                        )
                    }
                    item {
                        SearchFilterChip(
                            selected = state.resultFilter == SearchResultFilter.Genres,
                            label = R.string.filter_genres,
                            icon = Icons.Outlined.QueueMusic,
                            onClick = { viewModel.setResultFilter(SearchResultFilter.Genres) },
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
            ) {
                Text(
                    stringResource(R.string.filter_language),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            state.query.isBlank() -> SearchHistoryContent(
                history = state.history,
                onSelect = {
                    focusManager.clearFocus()
                    viewModel.selectHistory(it)
                },
                onRemove = viewModel::removeHistory,
                onClear = viewModel::clearHistory,
                modifier = Modifier.weight(1f),
            )
            state.loading && state.results.isEmpty() -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            state.failed -> SearchMessage(
                title = stringResource(R.string.search_error),
                body = stringResource(R.string.search_error_hint),
                action = { Button(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) } },
            )
            state.visibleResultCount == 0 -> SearchMessage(
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
                        text = stringResource(R.string.search_results_count, state.visibleResultCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = NavaSpacing.Xs),
                    )
                }
                if (state.loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                when (state.resultFilter) {
                    SearchResultFilter.All, SearchResultFilter.Tracks -> {
                        items(state.results, key = SearchTrack::id) { track ->
                            SearchTrackRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                onClick = {
                                    viewModel.submitSearch()
                                    focusManager.clearFocus()
                                    onTrackClick(track.toHomeTrack())
                                },
                                onOptions = {
                                    viewModel.submitSearch()
                                    focusManager.clearFocus()
                                    onTrackOptions(track.toHomeTrack())
                                },
                            )
                        }
                    }
                    SearchResultFilter.Artists -> items(state.artists, key = SearchArtistResult::name) { artist ->
                        SearchArtistRow(artist = artist, onClick = { viewModel.selectSuggestion(artist.name) })
                    }
                    SearchResultFilter.Genres -> items(state.genres, key = SearchGenreResult::name) { genre ->
                        SearchGenreRow(genre = genre, onClick = { viewModel.selectSuggestion(genre.name) })
                    }
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
private fun SearchHistoryContent(
    history: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) {
        SearchMessage(
            title = stringResource(R.string.search_start_title),
            body = stringResource(R.string.search_start_subtitle),
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = NavaSpacing.Lg,
            end = NavaSpacing.Lg,
            top = NavaSpacing.Sm,
            bottom = NavaSpacing.Xl,
        ),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.recent_searches), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.search_history_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = onClear) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(NavaSpacing.Xs))
                    Text(stringResource(R.string.clear_search_history))
                }
            }
        }
        items(history, key = { it.lowercase() }) { query ->
            Surface(
                onClick = { onSelect(query) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = NavaSpacing.Xs,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = NavaSpacing.Md, top = NavaSpacing.Xs, bottom = NavaSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.padding(NavaSpacing.Sm),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(query, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { onRemove(query) }) {
                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = stringResource(R.string.remove_search_history, query),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchArtistRow(artist: SearchArtistResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            AsyncImage(
                model = artist.coverImageUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier.size(68.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(artist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.artist_result_tracks, artist.trackCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.search_artist, artist.name))
        }
    }
}

@Composable
private fun SearchGenreRow(genre: SearchGenreResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Surface(modifier = Modifier.size(58.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(genre.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.genre_result_tracks, genre.trackCount),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.search_genre, genre.name))
        }
    }
}

@Composable
private fun SearchFilterChip(
    selected: Boolean,
    @StringRes label: Int,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = icon?.let { imageVector ->
            { Icon(imageVector, contentDescription = null, modifier = Modifier.size(18.dp)) }
        },
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
    icon: ImageVector = Icons.Outlined.ManageSearch,
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
                    imageVector = icon,
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
    quickViewModel: HomeQuickViewModel = hiltViewModel(),
    likedTracks: List<HomeTrack>,
    currentTrackId: String?,
    onPlay: (HomeTrack) -> Unit,
    onQueue: (HomeTrack) -> Unit,
    onTrackOptions: (HomeTrack) -> Unit,
    onOpenMyPlaylists: () -> Unit,
    onOpenTopPlaylists: () -> Unit,
    onShuffleSource: (List<HomeTrack>) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val quickState by quickViewModel.state.collectAsState()
    var quickCollection by rememberSaveable { mutableStateOf<HomeQuickCollection?>(null) }
    quickCollection?.let { collection ->
        HomeCollectionScreen(
            modifier = modifier,
            title = stringResource(if (collection == HomeQuickCollection.LIKED) R.string.quick_liked else R.string.quick_recent),
            subtitle = stringResource(if (collection == HomeQuickCollection.LIKED) R.string.liked_collection_subtitle else R.string.recent_collection_subtitle),
            tracks = if (collection == HomeQuickCollection.LIKED) likedTracks else quickState.recentTracks,
            loading = collection == HomeQuickCollection.RECENT && quickState.loadingRecent,
            failed = collection == HomeQuickCollection.RECENT && quickState.recentFailed,
            emptyTitle = if (collection == HomeQuickCollection.LIKED) R.string.liked_collection_empty else R.string.recent_collection_empty,
            emptyHint = if (collection == HomeQuickCollection.LIKED) R.string.liked_collection_empty_hint else R.string.recent_collection_empty_hint,
            currentTrackId = currentTrackId,
            onBack = { quickCollection = null },
            onRetry = quickViewModel::reloadRecent,
            onPlay = onPlay,
            onOptions = onTrackOptions,
        )
        return
    }
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
            is HomeUiState.Content -> homeContent(
                state = current,
                onPlay = onPlay,
                onQueue = onQueue,
                onLiked = { quickCollection = HomeQuickCollection.LIKED },
                onRecent = {
                    quickViewModel.reloadRecent()
                    quickCollection = HomeQuickCollection.RECENT
                },
                onMyPlaylists = onOpenMyPlaylists,
                onTopPlaylists = onOpenTopPlaylists,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeContent(
    state: HomeUiState.Content,
    onPlay: (HomeTrack) -> Unit,
    onQueue: (HomeTrack) -> Unit,
    onLiked: () -> Unit,
    onRecent: () -> Unit,
    onMyPlaylists: () -> Unit,
    onTopPlaylists: () -> Unit,
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
        QuickActionsGrid(onLiked, onRecent, onMyPlaylists, onTopPlaylists)
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
private fun DownloadsShell(
    modifier: Modifier,
    state: DownloadsUiState,
    currentTrackId: String?,
    viewModel: DownloadViewModel,
    onPlay: (OfflineTrackEntity) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = NavaSpacing.Lg,
            end = NavaSpacing.Lg,
            top = NavaSpacing.Lg,
            bottom = NavaSpacing.Xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
    ) {
        item {
            Text(stringResource(R.string.downloads), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(NavaSpacing.Xs))
            Text(
                stringResource(R.string.downloads_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.downloads.isEmpty() && state.activeDownloads.isEmpty()) {
            item { DownloadsEmptyCard() }
        } else {
            if (state.activeDownloads.isNotEmpty()) {
                item { Text(stringResource(R.string.downloading_now), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(state.activeDownloads, key = { "active-${it.trackId}" }) { track ->
                    ActiveDownloadCard(track)
                }
            }
            if (state.downloads.isNotEmpty()) {
                item { Text(stringResource(R.string.available_offline), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(state.downloads, key = OfflineTrackEntity::trackId) { track ->
                    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.remove(track)
                            true
                        } else value == SwipeToDismissBoxValue.Settled
                    })
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.large)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = NavaSpacing.Lg),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(R.string.swipe_to_remove),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.width(NavaSpacing.Sm))
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        },
                    ) {
                        DownloadedTrackCard(
                            track = track,
                            isCurrent = track.trackId == currentTrackId,
                            onClick = { onPlay(track) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(80.dp)) {
                Icon(
                    Icons.Outlined.DownloadDone,
                    contentDescription = null,
                    modifier = Modifier.padding(NavaSpacing.Lg),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(stringResource(R.string.downloads_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.downloads_empty_hint),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActiveDownloadCard(track: com.example.nava.data.downloads.DownloadTransfer) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            AsyncImage(
                model = track.coverImageUrl,
                contentDescription = stringResource(R.string.track_artwork, track.title),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier.size(NavaDimensions.SearchTrackArtworkSize).clip(MaterialTheme.shapes.medium),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artistName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(progress = { track.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.download_progress, track.progressPercent), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DownloadedTrackCard(track: OfflineTrackEntity, isCurrent: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                    model = track.coverImageUrl,
                    contentDescription = stringResource(R.string.track_artwork, track.title),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    modifier = Modifier.size(NavaDimensions.SearchTrackArtworkSize).clip(MaterialTheme.shapes.medium),
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(NavaDimensions.SearchPlayBadgeSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(R.string.play_downloaded_track, track.title),
                        modifier = Modifier.padding(NavaSpacing.Xs),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artistName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.downloaded_file_size, formatDownloadSize(track.byteCount)),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Outlined.DownloadDone,
                contentDescription = stringResource(R.string.available_offline),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatDownloadSize(byteCount: Long): String =
    DecimalFormat("0.0").format(byteCount / (1024.0 * 1024.0))

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
private fun QuickActionsGrid(
    onLiked: () -> Unit,
    onRecent: () -> Unit,
    onMyPlaylists: () -> Unit,
    onTopPlaylists: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NavaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
            QuickAction(R.string.quick_liked, R.string.quick_liked_subtitle, Icons.Outlined.FavoriteBorder, Modifier.weight(1f), onLiked)
            QuickAction(R.string.quick_recent, R.string.quick_recent_subtitle, Icons.Outlined.History, Modifier.weight(1f), onRecent)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
            QuickAction(R.string.quick_playlists, R.string.quick_playlists_subtitle, Icons.Outlined.QueueMusic, Modifier.weight(1f), onMyPlaylists)
            QuickAction(R.string.quick_top_playlists, R.string.quick_top_playlists_subtitle, Icons.Outlined.Public, Modifier.weight(1f), onTopPlaylists)
        }
    }
}

@Composable
private fun QuickAction(
    @StringRes label: Int,
    @StringRes subtitle: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(104.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = NavaSpacing.Xs,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(NavaSpacing.Md),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(text = stringResource(label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = stringResource(subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private enum class HomeQuickCollection { LIKED, RECENT }

@Composable
private fun HomeCollectionScreen(
    modifier: Modifier,
    title: String,
    subtitle: String,
    tracks: List<HomeTrack>,
    loading: Boolean,
    failed: Boolean,
    @StringRes emptyTitle: Int,
    @StringRes emptyHint: Int,
    currentTrackId: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (HomeTrack) -> Unit,
    onOptions: (HomeTrack) -> Unit,
) {
    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = NavaSpacing.Lg,
            end = NavaSpacing.Lg,
            top = NavaSpacing.Sm,
            bottom = NavaSpacing.Xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        when {
            loading -> item { Box(Modifier.fillParentMaxHeight(.55f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            failed -> item { HomeCollectionMessage(R.string.home_collection_error, R.string.home_collection_error_hint, onRetry) }
            tracks.isEmpty() -> item { HomeCollectionMessage(emptyTitle, emptyHint) }
            else -> items(tracks, key = HomeTrack::id) { track ->
                HomeCollectionTrackRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    onClick = { onPlay(track) },
                    onOptions = { onOptions(track) },
                )
            }
        }
    }
}

@Composable
private fun HomeCollectionMessage(@StringRes title: Int, @StringRes hint: Int, onRetry: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
        ) {
            Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(hint), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            onRetry?.let { Button(onClick = it) { Text(stringResource(R.string.retry)) } }
        }
    }
}

@Composable
private fun HomeCollectionTrackRow(track: HomeTrack, isCurrent: Boolean, onClick: () -> Unit, onOptions: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(track.coverImageUrl).crossfade(true).build(),
                    contentDescription = stringResource(R.string.track_artwork, track.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(NavaDimensions.SearchTrackArtworkSize).clip(MaterialTheme.shapes.medium),
                )
                Surface(modifier = Modifier.align(Alignment.BottomEnd).size(28.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(NavaSpacing.Xs))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artistName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onOptions) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.track_more_options, track.title)) }
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
    onOpenSocial: (SocialSection) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::selectAvatar)
    }
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(NavaSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs),
                ) {
                    Box(modifier = Modifier.size(NavaDimensions.ProfileAvatarCanvasSize)) {
                        UserAvatar(
                            model = ImageRequest.Builder(context)
                                .data(state.pendingAvatarUri ?: state.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.user_avatar),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(NavaDimensions.ProfileAvatarSize)
                                .border(
                                    NavaDimensions.ProfileAvatarBorderWidth,
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape,
                                ),
                        )
                        if (state.isEditing) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).size(NavaDimensions.ProfileAvatarActionSize),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shadowElevation = NavaSpacing.Xs,
                            ) {
                                IconButton(
                                    onClick = { avatarPicker.launch("image/*") },
                                    enabled = !state.isSaving,
                                ) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.edit_profile_photo),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        if (state.hasChanges) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).size(NavaDimensions.ProfileAvatarActionSize),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = NavaSpacing.Xs,
                            ) {
                                IconButton(
                                    onClick = viewModel::saveProfile,
                                    enabled = !state.isSaving && state.displayName.trim().length in 2..60,
                                ) {
                                    if (state.isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(NavaSpacing.Xl),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = NavaDimensions.AuthProgressStrokeWidth,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = stringResource(R.string.confirm_profile_changes),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = state.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = session.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (state.isPremium) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = stringResource(if (state.isPremium) R.string.premium_member else R.string.standard_member),
                            modifier = Modifier.padding(horizontal = NavaSpacing.Md, vertical = NavaSpacing.Xs),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isPremium) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!state.isEditing) {
                        FilledTonalButton(onClick = viewModel::startEditing) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                            Spacer(Modifier.size(NavaSpacing.Sm))
                            Text(stringResource(R.string.edit_profile))
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                    PersonalProfileStat(state.followersCount.toString(), R.string.followers, Modifier.weight(1f)) {
                        onOpenSocial(SocialSection.FOLLOWERS)
                    }
                    PersonalProfileStat(state.followingCount.toString(), R.string.following, Modifier.weight(1f)) {
                        onOpenSocial(SocialSection.FOLLOWING)
                    }
                    PersonalProfileStat(state.publicPlaylistsCount.toString(), R.string.playlists, Modifier.weight(1f)) {
                        onOpenSocial(SocialSection.PLAYLISTS)
                    }
                }
            }
            if (state.isEditing) {
                item {
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = viewModel::changeDisplayName,
                        enabled = !state.isSaving,
                        label = { Text(stringResource(R.string.display_name)) },
                        supportingText = { Text(stringResource(R.string.profile_changes_hint)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedButton(
                        onClick = viewModel::cancelEditing,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.cancel_editing)) }
                }
            }
            if (!state.isPremium) {
                item {
                    FilledTonalButton(
                        onClick = viewModel::upgrade,
                        enabled = !state.isSaving && !state.hasChanges,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.upgrade_demo)) }
                }
            }
            item {
                Surface(
                    onClick = { onOpenSocial(SocialSection.PEOPLE) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                    ) {
                        Surface(
                            modifier = Modifier.size(NavaDimensions.ProfileDiscoverIconSize),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.padding(NavaSpacing.Md))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                            Text(stringResource(R.string.discover_people), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.discover_people_subtitle), style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
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
private fun PersonalProfileStat(
    value: String,
    @StringRes label: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(vertical = NavaSpacing.Md, horizontal = NavaSpacing.Sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs),
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChatNotificationsScreen(
    modifier: Modifier,
    conversations: List<ChatConversation>,
    loading: Boolean,
    error: Boolean,
    onRetry: () -> Unit,
    onOpenConversation: (ChatConversation) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = NavaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
            Text(
                text = stringResource(R.string.unread_messages),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.unread_messages_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            loading && conversations.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            error && conversations.isEmpty() -> SearchMessage(
                title = stringResource(R.string.notifications_load_error),
                body = stringResource(R.string.social_error_hint),
                action = {
                    Button(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                },
            )
            conversations.isEmpty() -> SearchMessage(
                title = stringResource(R.string.no_unread_messages),
                body = stringResource(R.string.no_unread_messages_hint),
                icon = Icons.Outlined.NotificationsNone,
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = NavaSpacing.Lg),
            ) {
                items(conversations, key = ChatConversation::id) { conversation ->
                    Card(
                        onClick = { onOpenConversation(conversation) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(NavaSpacing.Md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = conversation.peerName.firstOrNull()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = conversation.peerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = conversation.lastMessage ?: stringResource(R.string.shared_music),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = NavaSpacing.Sm, vertical = NavaSpacing.Xs),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = stringResource(R.string.open_chat),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialShell(
    modifier: Modifier,
    onBack: () -> Unit,
    initialSection: SocialSection = SocialSection.PEOPLE,
    viewModel: SocialViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var messagesOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialSection) { viewModel.select(initialSection) }
    BackHandler {
        when {
            messagesOpen -> messagesOpen = false
            state.selectedPerson != null -> viewModel.closeProfile()
            else -> onBack()
        }
    }
    if (messagesOpen) {
        ChatShell(
            modifier = modifier,
            onBack = { messagesOpen = false },
            viewModel = chatViewModel,
            playbackViewModel = playbackViewModel,
        )
        return
    }
    state.selectedPerson?.let { person ->
        PublicProfileScreen(
            modifier = modifier,
            person = state.profileDetails?.person ?: person,
            details = state.profileDetails,
            loading = state.profileLoading,
            onBack = viewModel::closeProfile,
            onToggleFollow = { viewModel.toggleFollow(state.profileDetails?.person ?: person) },
            onMessage = {
                chatViewModel.open(state.profileDetails?.person ?: person)
                messagesOpen = true
            },
        )
        return
    }
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = NavaSpacing.Lg, vertical = NavaSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
        ) {
            Text(stringResource(R.string.discover_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.discover_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = { messagesOpen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Chat, contentDescription = null)
                Spacer(Modifier.size(NavaSpacing.Sm))
                Text(stringResource(R.string.messages))
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                item { SocialFilterChip(state.section == SocialSection.PEOPLE, R.string.people) { viewModel.select(SocialSection.PEOPLE) } }
                item { SocialFilterChip(state.section == SocialSection.FOLLOWING, R.string.following) { viewModel.select(SocialSection.FOLLOWING) } }
                item { SocialFilterChip(state.section == SocialSection.FOLLOWERS, R.string.followers) { viewModel.select(SocialSection.FOLLOWERS) } }
                item { SocialFilterChip(state.section == SocialSection.PLAYLISTS, R.string.top_public_playlists) { viewModel.select(SocialSection.PLAYLISTS) } }
            }
            if (state.section == SocialSection.PEOPLE) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::search,
                    placeholder = { Text(stringResource(R.string.search_people)) },
                    leadingIcon = { Icon(Icons.Outlined.ManageSearch, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.search("") }) {
                                Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        when {
            state.loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error -> SearchMessage(
                title = stringResource(R.string.social_error),
                body = stringResource(R.string.social_error_hint),
                action = { Button(onClick = { viewModel.select(state.section) }) { Text(stringResource(R.string.retry)) } },
            )
            state.section == SocialSection.PLAYLISTS && state.playlists.isEmpty() -> SearchMessage(
                title = stringResource(R.string.playlists_empty),
                body = stringResource(R.string.top_public_playlists_empty_hint),
            )
            state.people.isEmpty() && state.section != SocialSection.PLAYLISTS -> SearchMessage(
                title = stringResource(R.string.people_empty),
                body = stringResource(R.string.people_empty_hint),
            )
            state.section == SocialSection.PLAYLISTS -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NavaSpacing.Lg, vertical = NavaSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
            ) {
                items(state.playlists, key = PublicPlaylist::id) { playlist -> PublicPlaylistCard(playlist) }
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NavaSpacing.Lg, vertical = NavaSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
            ) {
                items(state.people, key = SocialPerson::id) { person ->
                    SocialPersonCard(
                        person = person,
                        onOpen = { viewModel.openProfile(person) },
                        onToggleFollow = { viewModel.toggleFollow(person) },
                        onMessage = {
                            chatViewModel.open(person)
                            messagesOpen = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialFilterChip(selected: Boolean, @StringRes label: Int, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                stringResource(label),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        },
    )
}

@Composable
private fun SocialPersonCard(
    person: SocialPerson,
    onOpen: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessage: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            UserAvatar(
                model = person.avatarUrl,
                contentDescription = stringResource(R.string.person_avatar, person.displayName),
                modifier = Modifier.size(NavaDimensions.SocialAvatarSize),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(person.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.view_profile),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onMessage) {
                Icon(Icons.Outlined.Chat, contentDescription = stringResource(R.string.open_chat))
            }
            if (person.isFollowing) {
                OutlinedButton(onClick = onToggleFollow) { Text(stringResource(R.string.following)) }
            } else {
                FilledTonalButton(onClick = onToggleFollow) { Text(stringResource(R.string.follow)) }
            }
        }
    }
}

@Composable
private fun PublicProfileScreen(
    modifier: Modifier,
    person: SocialPerson,
    details: SocialProfileDetails?,
    loading: Boolean,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessage: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(NavaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg),
    ) {
        item {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back_to_people))
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
            ) {
                UserAvatar(
                    model = person.avatarUrl,
                    contentDescription = stringResource(R.string.person_avatar, person.displayName),
                    modifier = Modifier
                        .size(NavaDimensions.PublicProfileAvatarSize)
                        .border(
                            NavaDimensions.ProfileAvatarBorderWidth,
                            MaterialTheme.colorScheme.primary,
                            CircleShape,
                        ),
                )
                Text(person.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(stringResource(R.string.public_profile), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (person.isFollowing) {
                    OutlinedButton(onClick = onToggleFollow) { Text(stringResource(R.string.unfollow)) }
                } else {
                    Button(onClick = onToggleFollow) { Text(stringResource(R.string.follow)) }
                }
                FilledTonalButton(onClick = onMessage) {
                    Icon(Icons.Outlined.Chat, contentDescription = null)
                    Spacer(Modifier.size(NavaSpacing.Sm))
                    Text(stringResource(R.string.messages))
                }
            }
        }
        if (loading) {
            item { Box(modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Xl), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else {
            details?.let { profile ->
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                        ProfileStat(profile.followersCount.toString(), R.string.followers, Modifier.weight(1f))
                        ProfileStat(profile.followingCount.toString(), R.string.following, Modifier.weight(1f))
                        ProfileStat(profile.playlists.size.toString(), R.string.playlists, Modifier.weight(1f))
                    }
                }
                item {
                    Text(stringResource(R.string.public_playlists), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (profile.playlists.isEmpty()) {
                    item { Text(stringResource(R.string.public_playlists_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(profile.playlists, key = PublicPlaylist::id) { playlist -> PublicPlaylistCard(playlist) }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, @StringRes label: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(NavaSpacing.Md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun PublicPlaylistCard(playlist: PublicPlaylist) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
            Text(playlist.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(playlist.ownerName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            playlist.description?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.track_count, playlist.trackCount), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun UserAvatar(model: Any?, contentDescription: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        fallback = painterResource(R.drawable.ic_launcher_foreground),
        error = painterResource(R.drawable.ic_launcher_foreground),
        modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsShell(
    session: AuthSession,
    preferences: UserPreferences,
    onEvent: (NavaEvent) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(NavaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SettingButtons(
                icon = Icons.Outlined.DarkMode,
                title = R.string.theme,
                options = listOf(
                    R.string.theme_system to ThemeMode.SYSTEM,
                    R.string.theme_light to ThemeMode.LIGHT,
                    R.string.theme_dark to ThemeMode.DARK,
                ),
                selected = preferences.themeMode,
                onSelected = { onEvent(NavaEvent.SetTheme(it)) },
            )
        }
        item {
            SettingButtons(
                icon = Icons.Outlined.Language,
                title = R.string.language,
                options = listOf(
                    R.string.language_english to AppLanguage.ENGLISH,
                    R.string.language_persian to AppLanguage.PERSIAN,
                ),
                selected = preferences.language,
                onSelected = { onEvent(NavaEvent.SetLanguage(it)) },
            )
        }
        item {
            SettingButtons(
                icon = Icons.Outlined.FormatSize,
                title = R.string.font_size,
                options = listOf(
                    R.string.font_size_small to FontScale.SMALL,
                    R.string.font_size_standard to FontScale.STANDARD,
                    R.string.font_size_large to FontScale.LARGE,
                ),
                selected = preferences.fontScale,
                onSelected = { onEvent(NavaEvent.SetFontScale(it)) },
            )
        }
        item { SignOutCard(email = session.email, onSignOut = { onEvent(NavaEvent.SignOut) }) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T> SettingButtons(
    icon: ImageVector,
    @StringRes title: Int,
    options: List<Pair<Int, T>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
                Surface(
                    modifier = Modifier.size(NavaDimensions.SettingsIconSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.padding(NavaSpacing.Sm))
                }
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm), modifier = Modifier.fillMaxWidth()) {
                options.forEach { (label, value) ->
                    val isSelected = selected == value
                    Surface(
                        onClick = { onSelected(value) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Text(
                            stringResource(label),
                            modifier = Modifier.padding(horizontal = NavaSpacing.Sm, vertical = NavaSpacing.Md),
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignOutCard(email: String, onSignOut: () -> Unit) {
    var confirmationOpen by rememberSaveable { mutableStateOf(false) }
    Surface(
        onClick = { confirmationOpen = true },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Md),
        ) {
            Surface(
                modifier = Modifier.size(NavaDimensions.SettingsAccountIconSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.padding(NavaSpacing.Md))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
                Text(stringResource(R.string.sign_out), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(email, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.sign_out_subtitle), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
    if (confirmationOpen) {
        AlertDialog(
            onDismissRequest = { confirmationOpen = false },
            icon = { Icon(Icons.Outlined.Logout, contentDescription = null) },
            title = { Text(stringResource(R.string.sign_out_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.sign_out_confirm_message)) },
            dismissButton = {
                OutlinedButton(onClick = { confirmationOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(onClick = {
                    confirmationOpen = false
                    onSignOut()
                }) {
                    Text(stringResource(R.string.sign_out))
                }
            },
        )
    }
}
