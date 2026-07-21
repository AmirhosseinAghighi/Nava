package com.example.nava.domain.preferences

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage { SYSTEM, ENGLISH, PERSIAN }
enum class FontScale { SMALL, STANDARD, LARGE }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontScale: FontScale = FontScale.STANDARD,
)

interface PreferencesRepository {
    val preferences: kotlinx.coroutines.flow.Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setFontScale(scale: FontScale)
}
