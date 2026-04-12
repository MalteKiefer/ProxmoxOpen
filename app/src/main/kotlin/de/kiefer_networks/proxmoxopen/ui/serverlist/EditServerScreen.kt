package de.kiefer_networks.proxmoxopen.ui.serverlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.HeroHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServerScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditServerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_server_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroHeader(title = state.name.ifBlank { "Server" }, subtitle = "${state.host}:${state.port}", icon = Icons.Outlined.Edit)
            OutlinedTextField(value = state.name, onValueChange = viewModel::onName, label = { Text(stringResource(R.string.field_server_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.host, onValueChange = viewModel::onHost, label = { Text(stringResource(R.string.field_host)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.port, onValueChange = viewModel::onPort, label = { Text(stringResource(R.string.field_port)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.username, onValueChange = viewModel::onUsername, label = { Text(stringResource(R.string.field_username)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.tokenId, onValueChange = viewModel::onTokenId, label = { Text(stringResource(R.string.field_token_id)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}
