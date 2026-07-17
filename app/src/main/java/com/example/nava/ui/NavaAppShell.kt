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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.nava.R
import com.example.nava.domain.auth.AuthSession
import com.example.nava.domain.preferences.AppLanguage
import com.example.nava.domain.preferences.ThemeMode
import com.example.nava.domain.preferences.UserPreferences
import com.example.nava.ui.theme.NavaSpacing

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
        },
    ) { padding ->
        when (selectedIndex) {
            0 -> HomeShell(modifier = Modifier.padding(padding))
            4 -> ProfileShell(session, preferences, onEvent, Modifier.padding(padding))
            else -> PlaceholderShell(navigationItems[selectedIndex].title, Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeShell(modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(NavaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg)) {
        Text(stringResource(R.string.home_welcome), style = MaterialTheme.typography.headlineSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm)) {
            item { QuickAction(R.string.quick_liked) }
            item { QuickAction(R.string.quick_recent) }
            item { QuickAction(R.string.quick_playlists) }
            item { QuickAction(R.string.quick_artists) }
        }
        Text(stringResource(R.string.shell_placeholder), style = MaterialTheme.typography.bodyLarge)
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
