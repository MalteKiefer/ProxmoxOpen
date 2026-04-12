package de.kiefer_networks.proxmoxopen.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.Server
import de.kiefer_networks.proxmoxopen.domain.repository.AuthRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.domain.usecase.LoginUseCase
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverName: String = "",
    val username: String = "",
    val realm: Realm = Realm.PAM,
    val password: String = "",
    val totp: String = "",
    val hasStoredPassword: Boolean = false,
    val isSigningIn: Boolean = false,
    val error: ApiError? = null,
    val signedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val authRepository: AuthRepository,
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    val serverId: Long = savedStateHandle.toRoute<Route.Login>().serverId

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val server = serverRepository.getById(serverId) ?: return@launch
            val storedPassword = serverRepository.getPassword(serverId)
            _state.update {
                it.copy(
                    serverName = server.name,
                    username = server.username ?: "",
                    realm = server.realm,
                    password = storedPassword.orEmpty(),
                    hasStoredPassword = storedPassword != null,
                )
            }

            // If we already have an active session, skip the form.
            val existing = authRepository.currentSession(serverId)
            if (existing != null) {
                _state.update { it.copy(signedIn = true) }
                return@launch
            }

            // Token-based servers log in automatically with the stored secret.
            if (server.realm == Realm.PVE_TOKEN) {
                val tokenSecret = serverRepository.getTokenSecret(serverId)
                if (tokenSecret == null) {
                    _state.update { it.copy(error = ApiError.Auth("token secret missing")) }
                    return@launch
                }
                attemptTokenLogin(server, tokenSecret)
                return@launch
            }

            // If we have a stored password and no TOTP is required, try auto-login.
            if (storedPassword != null) {
                attemptLogin(server, storedPassword, totp = null)
            }
        }
    }

    private suspend fun attemptTokenLogin(server: Server, tokenSecret: String) {
        _state.update { it.copy(isSigningIn = true, error = null) }
        val creds = Credentials.ApiToken(
            username = server.username ?: "root",
            realm = server.realm,
            tokenId = server.tokenId ?: "",
            tokenSecret = tokenSecret,
        )
        when (val result = loginUseCase(serverId, creds)) {
            is ApiResult.Success ->
                _state.update { it.copy(isSigningIn = false, signedIn = true) }
            is ApiResult.Failure ->
                _state.update { it.copy(isSigningIn = false, error = result.error) }
        }
    }

    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onTotp(value: String) = _state.update { it.copy(totp = value.filter { c -> c.isDigit() }, error = null) }

    fun signIn() {
        val s = _state.value
        viewModelScope.launch {
            val server = serverRepository.getById(serverId) ?: return@launch
            attemptLogin(server, s.password, s.totp.takeIf { it.isNotBlank() })
        }
    }

    private suspend fun attemptLogin(server: Server, password: String, totp: String?) {
        _state.update { it.copy(isSigningIn = true, error = null) }
        val creds = Credentials.UserPassword(
            username = server.username ?: "",
            realm = server.realm,
            password = password,
            totp = totp,
        )
        when (val result = loginUseCase(serverId, creds)) {
            is ApiResult.Success ->
                _state.update { it.copy(isSigningIn = false, signedIn = true) }
            is ApiResult.Failure ->
                _state.update { it.copy(isSigningIn = false, error = result.error) }
        }
    }
}
