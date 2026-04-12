package app.proxmoxopen.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.proxmoxopen.preferences.LanguageOption
import app.proxmoxopen.preferences.ThemeMode
import app.proxmoxopen.preferences.UserPreferences
import app.proxmoxopen.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
    }

    fun setLanguage(language: LanguageOption) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAppLockEnabled(enabled) }
    }
}
