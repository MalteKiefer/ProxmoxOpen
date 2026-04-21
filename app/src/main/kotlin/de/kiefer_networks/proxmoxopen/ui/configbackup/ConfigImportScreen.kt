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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.serialization.json.Json

data class ConfigImportUiState(
    val pickedUri: Uri? = null,
    val passphrase: String = "",
    val decryptedConfig: ExportedConfig? = null,
    val isWorking: Boolean = false,
    val showStrategyDialog: Boolean = false,
    val errorRes: Int? = null,
    val successRes: Int? = null,
)

@HiltViewModel
class ConfigImportViewModel @Inject constructor(
    private val repo: ConfigBackupRepository,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(ConfigImportUiState())
    val state: StateFlow<ConfigImportUiState> = _state.asStateFlow()

    fun onPassphrase(v: String) = _state.update { it.copy(passphrase = v, errorRes = null) }

    fun clearMessages() = _state.update { it.copy(errorRes = null, successRes = null) }

    fun onFilePicked(context: Context, uri: Uri?) {
        if (uri == null) return
        _state.update {
            it.copy(
                pickedUri = uri,
                decryptedConfig = null,
                passphrase = "",
                errorRes = null,
                successRes = null,
            )
        }
        // Cheap up-front validation: read first bytes to confirm the magic header.
        viewModelScope.launch {
            val header = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val buf = ByteArray(ConfigBackupCrypto.MAGIC.length)
                        val read = stream.read(buf)
                        if (read == buf.size) buf else ByteArray(0)
                    } ?: ByteArray(0)
                }.getOrDefault(ByteArray(0))
            }
            if (!ConfigBackupCrypto.looksLikeEnvelope(header)) {
                _state.update {
                    it.copy(pickedUri = null, errorRes = R.string.backup_bad_file)
                }
            }
        }
    }

    fun decrypt(context: Context) {
        val s = _state.value
        val uri = s.pickedUri ?: return
        if (s.passphrase.isEmpty()) return
        _state.update { it.copy(isWorking = true, errorRes = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val bytes = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: ByteArray(0)
                }.getOrElse { return@withContext DecryptOutcome.BadFile }
                if (bytes.isEmpty() || !ConfigBackupCrypto.looksLikeEnvelope(bytes)) {
                    return@withContext DecryptOutcome.BadFile
                }
                val pass = s.passphrase.toCharArray()
                try {
                    val plain = ConfigBackupCrypto.decrypt(bytes, pass)
                    val config = runCatching {
                        json.decodeFromString(ExportedConfig.serializer(), String(plain, Charsets.UTF_8))
                    }.getOrElse { return@withContext DecryptOutcome.BadFile }
                    DecryptOutcome.Ok(config)
                } catch (_: ConfigBackupCrypto.WrongPassphraseException) {
                    DecryptOutcome.WrongPassphrase
                } catch (_: ConfigBackupCrypto.BadFileException) {
                    DecryptOutcome.BadFile
                } finally {
                    pass.fill('\u0000')
                }
            }
            when (result) {
                is DecryptOutcome.Ok -> _state.update {
                    it.copy(
                        isWorking = false,
                        decryptedConfig = result.config,
                        showStrategyDialog = true,
                    )
                }
                DecryptOutcome.WrongPassphrase -> _state.update {
                    it.copy(isWorking = false, errorRes = R.string.backup_wrong_passphrase)
                }
                DecryptOutcome.BadFile -> _state.update {
                    it.copy(isWorking = false, errorRes = R.string.backup_bad_file)
                }
            }
        }
    }

    fun confirmStrategy(strategy: ImportStrategy) {
        val config = _state.value.decryptedConfig ?: return
        _state.update { it.copy(isWorking = true, showStrategyDialog = false) }
        viewModelScope.launch {
            runCatching { repo.apply(config, strategy) }
                .onSuccess {
                    _state.update {
                        ConfigImportUiState(successRes = R.string.backup_import_success)
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(isWorking = false, errorRes = R.string.backup_bad_file)
                    }
                }
        }
    }

    fun dismissStrategy() {
        _state.update { it.copy(showStrategyDialog = false) }
    }

    private sealed class DecryptOutcome {
        data class Ok(val config: ExportedConfig) : DecryptOutcome()
        data object WrongPassphrase : DecryptOutcome()
        data object BadFile : DecryptOutcome()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigImportScreen(
    onBack: () -> Unit,
    viewModel: ConfigImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.onFilePicked(context, uri) }

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
                title = { Text(stringResource(R.string.backup_import)) },
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
                text = stringResource(R.string.backup_import_desc),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_import))
            }
            if (state.pickedUri != null) {
                OutlinedTextField(
                    value = state.passphrase,
                    onValueChange = viewModel::onPassphrase,
                    label = { Text(stringResource(R.string.backup_passphrase)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.decrypt(context) },
                    enabled = !state.isWorking && state.passphrase.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.backup_import))
                    }
                }
            }
        }
    }

    if (state.showStrategyDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissStrategy,
            title = { Text(stringResource(R.string.backup_import)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_import_desc))
                    Button(
                        onClick = { viewModel.confirmStrategy(ImportStrategy.Merge) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.backup_merge)) }
                    OutlinedButton(
                        onClick = { viewModel.confirmStrategy(ImportStrategy.Replace) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.backup_replace)) }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissStrategy) {
                    Text(stringResource(R.string.backup_cancel))
                }
            },
        )
    }
}
