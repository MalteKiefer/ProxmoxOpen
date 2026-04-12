package app.proxmoxopen.ui.settings

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
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import app.proxmoxopen.BuildConfig
import app.proxmoxopen.R
import app.proxmoxopen.preferences.LanguageOption
import app.proxmoxopen.preferences.ThemeMode
import app.proxmoxopen.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_section_appearance))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.BrightnessMedium,
                    title = stringResource(R.string.setting_theme),
                    subtitle = themeLabel(prefs.themeMode),
                    onClick = { showThemeDialog = true },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggle(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.setting_dynamic_color),
                        subtitle = stringResource(R.string.setting_dynamic_color_desc),
                        checked = prefs.useDynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                }
            }

            SectionTitle(stringResource(R.string.settings_section_language))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.setting_language),
                    subtitle = languageLabel(prefs.language),
                    onClick = { showLanguageDialog = true },
                )
            }

            SectionTitle(stringResource(R.string.settings_section_security))
            SettingsCard {
                SettingsToggle(
                    icon = Icons.Outlined.Fingerprint,
                    title = stringResource(R.string.setting_biometric_lock),
                    subtitle = stringResource(R.string.setting_biometric_lock_desc),
                    checked = prefs.appLockEnabled,
                    onCheckedChange = viewModel::setAppLock,
                )
            }

            SectionTitle(stringResource(R.string.settings_section_about))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about),
                    subtitle = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.about_source),
                    subtitle = "github.com/proxmoxopen",
                )
            }
        }
    }

    if (showThemeDialog) {
        OptionDialog(
            title = stringResource(R.string.setting_theme),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = prefs.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
        )
    }
    if (showLanguageDialog) {
        OptionDialog(
            title = stringResource(R.string.setting_language),
            options = LanguageOption.entries.map { it to languageLabel(it) },
            selected = prefs.language,
            onDismiss = { showLanguageDialog = false },
            onSelect = {
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    val base = Modifier.fillMaxWidth()
    val rowModifier = if (onClick != null) base.clickable(onClick = onClick) else base
    Row(
        modifier = rowModifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> OptionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) },
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
private fun languageLabel(language: LanguageOption): String = stringResource(
    when (language) {
        LanguageOption.SYSTEM -> R.string.setting_language_system
        LanguageOption.ENGLISH -> R.string.language_english
        LanguageOption.GERMAN -> R.string.language_german
    },
)
