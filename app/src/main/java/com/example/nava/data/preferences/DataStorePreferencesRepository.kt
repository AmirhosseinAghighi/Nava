package com.example.nava.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nava.domain.preferences.AppLanguage
import com.example.nava.domain.preferences.PreferencesRepository
import com.example.nava.domain.preferences.ThemeMode
import com.example.nava.domain.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "nava_settings")

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    private val context: Context,
) : PreferencesRepository {
    override val preferences: Flow<UserPreferences> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { stored ->
            UserPreferences(
                themeMode = stored[ThemeModeKey]?.toThemeMode() ?: ThemeMode.SYSTEM,
                language = stored[LanguageKey]?.toLanguage() ?: AppLanguage.SYSTEM,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[ThemeModeKey] = mode.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[LanguageKey] = language.name }
    }

    private fun String.toThemeMode() = ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM
    private fun String.toLanguage() = AppLanguage.entries.firstOrNull { it.name == this } ?: AppLanguage.SYSTEM

    private companion object {
        val ThemeModeKey = stringPreferencesKey("theme_mode")
        val LanguageKey = stringPreferencesKey("language")
    }
}
