package de.kiefer_networks.proxmoxopen.ui.configbackup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kiefer_networks.proxmoxopen.R
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class ConfigExportUiState(
    val passphrase: String = "",
    val confirm: String = "",
    val isWorking: Boolean = false,
    val pendingBytes: ByteArray? = null,
    val errorRes: Int? = null,
    val successRes: Int? = null,
)

@HiltViewModel
class ConfigExportViewModel @Inject constructor(
    private val repo: ConfigBackupRepository,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(ConfigExportUiState())
    val state: StateFlow<ConfigExportUiState> = _state.asStateFlow()

    fun onPassphrase(v: String) = _state.update { it.copy(passphrase = v, errorRes = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, errorRes = null) }

    fun clearMessages() = _state.update { it.copy(errorRes = null, successRes = null) }

    /** Validate + build envelope bytes in memory; caller then launches SAF to pick a URI. */
    fun prepare() {
        val s = _state.value
        if (s.passphrase.length < MIN_PASSPHRASE_LENGTH) {
            _state.update { it.copy(errorRes = R.string.backup_passphrase_too_short) }
            return
        }
        if (s.passphrase != s.confirm) {
            _state.update { it.copy(errorRes = R.string.backup_passphrase_mismatch) }
            return
        }
        _state.update { it.copy(isWorking = true, errorRes = null, successRes = null) }
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.Default) {
                val config = repo.collect()
                val plain = json.encodeToString(config).toByteArray(Charsets.UTF_8)
                val pass = s.passphrase.toCharArray()
                try {
                    ConfigBackupCrypto.encrypt(plain, pass)
                } finally {
                    pass.fill('\u0000')
                }
            }
            _state.update { it.copy(isWorking = false, pendingBytes = bytes) }
        }
    }

    fun onUriPicked(context: Context, uri: Uri) {
        val bytes = _state.value.pendingBytes ?: return
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                        ?: error("null output stream")
                }.isSuccess
            }
            _state.update {
                it.copy(
                    isWorking = false,
                    pendingBytes = null,
                    passphrase = "",
                    confirm = "",
                    successRes = if (ok) R.string.backup_export_success else null,
                    errorRes = if (!ok) R.string.backup_bad_file else null,
                )
            }
        }
    }

    fun onSaveCancelled() {
        _state.update { it.copy(isWorking = false, pendingBytes = null) }
    }

    companion object {
        const val MIN_PASSPHRASE_LENGTH = 8
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigExportScreen(
    onBack: () -> Unit,
    viewModel: ConfigExportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) viewModel.onUriPicked(context, uri) else viewModel.onSaveCancelled()
    }

    // When bytes are ready, launch the file-save chooser.
    LaunchedEffect(state.pendingBytes) {
        if (state.pendingBytes != null) {
            saveLauncher.launch("proxmoxopen-${System.currentTimeMillis()}.pmoconfig")
        }
    }

    val errorRes = state.errorRes
    val successRes = state.successRes
    LaunchedEffect(errorRes, successRes) {
        when {
            errorRes != null -> {
                snackbar.showSnackbar(context.getString(errorRes))
                viewModel.clearMessages()
            }
            successRes != null -> {
                snackbar.showSnackbar(context.getString(successRes))
                viewModel.clearMessages()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_export)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.backup_export_desc),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = state.passphrase,
                onValueChange = viewModel::onPassphrase,
                label = { Text(stringResource(R.string.backup_passphrase)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.confirm,
                onValueChange = viewModel::onConfirm,
                label = { Text(stringResource(R.string.backup_passphrase_confirm)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::prepare,
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.backup_export))
                }
            }
        }
    }
}
