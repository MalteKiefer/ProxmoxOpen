package de.kiefer_networks.proxmoxopen

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.core.ui.theme.ProxMoxOpenTheme
import de.kiefer_networks.proxmoxopen.preferences.LanguageOption
import de.kiefer_networks.proxmoxopen.preferences.ThemeMode
import de.kiefer_networks.proxmoxopen.ui.main.MainViewModel
import de.kiefer_networks.proxmoxopen.ui.nav.NavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()

            LaunchedEffect(prefs.language) {
                val tag = prefs.language.languageTag
                val locales = if (tag == null) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }
                if (AppCompatDelegate.getApplicationLocales() != locales) {
                    AppCompatDelegate.setApplicationLocales(locales)
                }
            }

            ProxMoxOpenTheme(
                useDarkTheme = when (prefs.themeMode) {
                    ThemeMode.SYSTEM -> null
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
                dynamicColor = prefs.useDynamicColor,
            ) {
                de.kiefer_networks.proxmoxopen.ui.applock.AppLockGate(enabled = prefs.appLockEnabled) {
                    NavGraph()
                }
            }
        }
    }
}
