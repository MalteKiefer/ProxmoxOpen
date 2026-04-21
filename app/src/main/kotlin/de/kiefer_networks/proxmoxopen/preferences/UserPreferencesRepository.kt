package de.kiefer_networks.proxmoxopen.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_prefs",
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.userPreferencesDataStore

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[THEME_KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            useDynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: false,
            amoledBlack = prefs[AMOLED_BLACK_KEY] ?: false,
            language = prefs[LANGUAGE_KEY]?.let { runCatching { LanguageOption.valueOf(it) }.getOrNull() }
                ?: LanguageOption.SYSTEM,
            appLockEnabled = prefs[APP_LOCK_KEY] ?: false,
            blockScreenshots = prefs[BLOCK_SCREENSHOTS_KEY] ?: false,
            refreshInterval = prefs[REFRESH_KEY]?.let { runCatching { RefreshInterval.valueOf(it) }.getOrNull() }
                ?: RefreshInterval.SEC_15,
            terminalFontSize = prefs[TERMINAL_FONT_KEY]?.let { runCatching { TerminalFontSize.valueOf(it) }.getOrNull() }
                ?: TerminalFontSize.MEDIUM,
            terminalTheme = prefs[TERMINAL_THEME_KEY]?.let { runCatching { TerminalTheme.valueOf(it) }.getOrNull() }
                ?: TerminalTheme.DARK,
            offlineCacheEnabled = prefs[OFFLINE_CACHE_KEY] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_KEY] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        dataStore.edit { it[AMOLED_BLACK_KEY] = enabled }
    }

    suspend fun setLanguage(language: LanguageOption) {
        dataStore.edit { it[LANGUAGE_KEY] = language.name }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[APP_LOCK_KEY] = enabled }
    }

    suspend fun setBlockScreenshots(enabled: Boolean) {
        dataStore.edit { it[BLOCK_SCREENSHOTS_KEY] = enabled }
    }

    suspend fun setRefreshInterval(interval: RefreshInterval) {
        dataStore.edit { it[REFRESH_KEY] = interval.name }
    }

    suspend fun setTerminalFontSize(size: TerminalFontSize) {
        dataStore.edit { it[TERMINAL_FONT_KEY] = size.name }
    }

    suspend fun setTerminalTheme(theme: TerminalTheme) {
        dataStore.edit { it[TERMINAL_THEME_KEY] = theme.name }
    }

    suspend fun setOfflineCacheEnabled(enabled: Boolean) {
        dataStore.edit { it[OFFLINE_CACHE_KEY] = enabled }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val AMOLED_BLACK_KEY = booleanPreferencesKey("amoled_black")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val APP_LOCK_KEY = booleanPreferencesKey("app_lock")
        private val BLOCK_SCREENSHOTS_KEY = booleanPreferencesKey("block_screenshots")
        private val REFRESH_KEY = stringPreferencesKey("refresh_interval")
        private val TERMINAL_FONT_KEY = stringPreferencesKey("terminal_font_size")
        private val TERMINAL_THEME_KEY = stringPreferencesKey("terminal_theme")
        private val OFFLINE_CACHE_KEY = booleanPreferencesKey("offline_cache_enabled")
    }
}
