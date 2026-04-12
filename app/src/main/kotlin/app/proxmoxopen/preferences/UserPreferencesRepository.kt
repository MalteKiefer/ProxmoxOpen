package app.proxmoxopen.preferences

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
            language = prefs[LANGUAGE_KEY]?.let { runCatching { LanguageOption.valueOf(it) }.getOrNull() }
                ?: LanguageOption.SYSTEM,
            appLockEnabled = prefs[APP_LOCK_KEY] ?: false,
            refreshInterval = prefs[REFRESH_KEY]?.let { runCatching { RefreshInterval.valueOf(it) }.getOrNull() }
                ?: RefreshInterval.SEC_15,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_KEY] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    suspend fun setLanguage(language: LanguageOption) {
        dataStore.edit { it[LANGUAGE_KEY] = language.name }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[APP_LOCK_KEY] = enabled }
    }

    suspend fun setRefreshInterval(interval: RefreshInterval) {
        dataStore.edit { it[REFRESH_KEY] = interval.name }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val APP_LOCK_KEY = booleanPreferencesKey("app_lock")
        private val REFRESH_KEY = stringPreferencesKey("refresh_interval")
    }
}
