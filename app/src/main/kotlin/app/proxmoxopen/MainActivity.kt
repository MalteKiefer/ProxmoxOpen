package app.proxmoxopen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.core.ui.theme.ProxMoxOpenTheme
import app.proxmoxopen.preferences.LanguageOption
import app.proxmoxopen.preferences.ThemeMode
import app.proxmoxopen.ui.main.MainViewModel
import app.proxmoxopen.ui.nav.NavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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

            // Default to dark Proxmox brand colors before the DataStore emits
            // its first value, to avoid a one-frame light flash.
            ProxMoxOpenTheme(
                useDarkTheme = when (prefs.themeMode) {
                    ThemeMode.SYSTEM -> null
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
                dynamicColor = prefs.useDynamicColor,
            ) {
                NavGraph()
            }
        }
    }
}
