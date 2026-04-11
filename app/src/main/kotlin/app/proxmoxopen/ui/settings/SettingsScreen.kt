package app.proxmoxopen.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.proxmoxopen.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.setting_dynamic_color_desc)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_biometric_lock)) },
                supportingContent = { Text(stringResource(R.string.setting_biometric_lock_desc)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_flag_secure)) },
                supportingContent = { Text(stringResource(R.string.setting_flag_secure_desc)) },
            )
            ListItem(headlineContent = { Text(stringResource(R.string.about)) })
        }
    }
}
