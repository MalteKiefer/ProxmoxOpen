package de.kiefer_networks.proxmoxopen.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.BuildConfig
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.preferences.LanguageOption
import de.kiefer_networks.proxmoxopen.preferences.RefreshInterval
import de.kiefer_networks.proxmoxopen.preferences.TerminalFontSize
import de.kiefer_networks.proxmoxopen.preferences.TerminalTheme
import de.kiefer_networks.proxmoxopen.preferences.ThemeMode
import de.kiefer_networks.proxmoxopen.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onAbout: (() -> Unit)? = null,
    showBackButton: Boolean = false,
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRefreshDialog by remember { mutableStateOf(false) }
    var showTerminalFontDialog by remember { mutableStateOf(false) }
    var showTerminalThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    if (showBackButton && onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_section_appearance))
            SettingsCard {
                SettingsRow(Icons.Outlined.BrightnessMedium, stringResource(R.string.setting_theme), themeLabel(prefs.themeMode)) { showThemeDialog = true }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggle(Icons.Outlined.Palette, stringResource(R.string.setting_dynamic_color), stringResource(R.string.setting_dynamic_color_desc), prefs.useDynamicColor, viewModel::setDynamicColor)
                }
            }
            SectionTitle(stringResource(R.string.settings_section_language))
            SettingsCard { SettingsRow(Icons.Outlined.Language, stringResource(R.string.setting_language), languageLabel(prefs.language)) { showLanguageDialog = true } }
            SectionTitle(stringResource(R.string.settings_section_data))
            SettingsCard {
                SettingsRow(Icons.Outlined.Refresh, stringResource(R.string.setting_refresh_interval), prefs.refreshInterval.label) { showRefreshDialog = true }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggle(Icons.Outlined.CloudOff, stringResource(R.string.offline_cache_title), stringResource(R.string.offline_cache_desc), prefs.offlineCacheEnabled, viewModel::setOfflineCacheEnabled)
            }
            SectionTitle(stringResource(R.string.settings_section_terminal))
            SettingsCard {
                SettingsRow(Icons.Outlined.Code, stringResource(R.string.setting_terminal_font_size), terminalFontLabel(prefs.terminalFontSize)) { showTerminalFontDialog = true }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(Icons.Outlined.Code, stringResource(R.string.setting_terminal_theme), terminalThemeLabel(prefs.terminalTheme)) { showTerminalThemeDialog = true }
            }
            SectionTitle(stringResource(R.string.settings_section_security))
            SettingsCard {
                SettingsToggle(Icons.Outlined.Fingerprint, stringResource(R.string.setting_biometric_lock), stringResource(R.string.setting_biometric_lock_desc), prefs.appLockEnabled, viewModel::setAppLock)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggle(Icons.Outlined.Security, stringResource(R.string.setting_block_screenshots), stringResource(R.string.setting_block_screenshots_desc), prefs.blockScreenshots, viewModel::setBlockScreenshots)
            }
            SectionTitle(stringResource(R.string.settings_section_about))
            SettingsCard {
                SettingsRow(Icons.Outlined.Info, stringResource(R.string.about), stringResource(R.string.about_version, BuildConfig.VERSION_NAME)) { onAbout?.invoke() }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(Icons.Outlined.Favorite, stringResource(R.string.donate_title), stringResource(R.string.donate_subtitle)) { onAbout?.invoke() }
            }
        }
    }
    if (showThemeDialog) OptionDialog(stringResource(R.string.setting_theme), ThemeMode.entries.map { it to themeLabel(it) }, prefs.themeMode, { showThemeDialog = false }) { viewModel.setThemeMode(it); showThemeDialog = false }
    if (showLanguageDialog) OptionDialog(stringResource(R.string.setting_language), LanguageOption.entries.map { it to languageLabel(it) }, prefs.language, { showLanguageDialog = false }) { viewModel.setLanguage(it); showLanguageDialog = false }
    if (showRefreshDialog) OptionDialog(stringResource(R.string.setting_refresh_interval), RefreshInterval.entries.map { it to it.label }, prefs.refreshInterval, { showRefreshDialog = false }) { viewModel.setRefreshInterval(it); showRefreshDialog = false }
    if (showTerminalFontDialog) OptionDialog(stringResource(R.string.setting_terminal_font_size), TerminalFontSize.entries.map { it to terminalFontLabel(it) }, prefs.terminalFontSize, { showTerminalFontDialog = false }) { viewModel.setTerminalFontSize(it); showTerminalFontDialog = false }
    if (showTerminalThemeDialog) OptionDialog(stringResource(R.string.setting_terminal_theme), TerminalTheme.entries.map { it to terminalThemeLabel(it) }, prefs.terminalTheme, { showTerminalThemeDialog = false }) { viewModel.setTerminalTheme(it); showTerminalThemeDialog = false }
}

@Composable private fun SectionTitle(text: String) { Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
@Composable private fun SettingsCard(content: @Composable () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) { Column { content() } } }
@Composable private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    val m = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    Row(modifier = m.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 16.dp).weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}
@Composable private fun SettingsToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 16.dp).weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked = checked, onCheckedChange = onCheckedChange) }
}
@Composable private fun <T> OptionDialog(title: String, options: List<Pair<T, String>>, selected: T, onDismiss: () -> Unit, onSelect: (T) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(Modifier.verticalScroll(rememberScrollState())) { options.forEach { (v, l) -> Row(Modifier.fillMaxWidth().clickable { onSelect(v) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = v == selected, onClick = { onSelect(v) }); Text(l, Modifier.padding(start = 8.dp)) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
}
@Composable private fun themeLabel(m: ThemeMode) = stringResource(when (m) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark })
@Composable private fun languageLabel(l: LanguageOption) = stringResource(when (l) { LanguageOption.SYSTEM -> R.string.setting_language_system; LanguageOption.ENGLISH -> R.string.language_english; LanguageOption.GERMAN -> R.string.language_german; LanguageOption.FRENCH -> R.string.language_french; LanguageOption.SPANISH -> R.string.language_spanish; LanguageOption.ITALIAN -> R.string.language_italian })
private fun terminalFontLabel(s: TerminalFontSize) = "${s.px}px"
private fun terminalThemeLabel(t: TerminalTheme) = when (t) {
    TerminalTheme.DARK -> "Dark"; TerminalTheme.LIGHT -> "Light"
    TerminalTheme.SOLARIZED_DARK -> "Solarized Dark"; TerminalTheme.SOLARIZED_LIGHT -> "Solarized Light"
    TerminalTheme.DRACULA -> "Dracula"; TerminalTheme.MONOKAI -> "Monokai"
    TerminalTheme.NORD -> "Nord"; TerminalTheme.GRUVBOX_DARK -> "Gruvbox Dark"
    TerminalTheme.GRUVBOX_LIGHT -> "Gruvbox Light"; TerminalTheme.ONE_DARK -> "One Dark"
    TerminalTheme.TOKYO_NIGHT -> "Tokyo Night"; TerminalTheme.CATPPUCCIN -> "Catppuccin Mocha"
    TerminalTheme.PROXMOX -> "Proxmox"
}
