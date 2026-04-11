package app.proxmoxopen.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.proxmoxopen.domain.model.Cluster
import app.proxmoxopen.domain.model.Credentials
import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.model.Realm
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
import app.proxmoxopen.domain.usecase.GetClusterUseCase
import app.proxmoxopen.domain.usecase.ListGuestsUseCase
import app.proxmoxopen.domain.usecase.LoginUseCase
import androidx.navigation.toRoute
import app.proxmoxopen.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val serverName: String = "",
    val cluster: Cluster? = null,
    val guests: List<Guest> = emptyList(),
    val isLoading: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCluster: GetClusterUseCase,
    private val listGuests: ListGuestsUseCase,
    private val login: LoginUseCase,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    val serverId: Long = savedStateHandle.toRoute<Route.Dashboard>().serverId

    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val server = serverRepository.getById(serverId)
            if (server == null) {
                _state.update {
                    it.copy(isLoading = false, error = ApiError.Unknown("server not found"))
                }
                return@launch
            }
            _state.update { it.copy(serverName = server.name) }

            // Token-based servers: login is a smoke-test with the token header.
            if (server.realm == Realm.PVE_TOKEN) {
                val tokenSecret = serverRepository.getTokenSecret(serverId)
                if (tokenSecret == null) {
                    _state.update {
                        it.copy(isLoading = false, error = ApiError.Auth("token missing"))
                    }
                    return@launch
                }
                val creds = Credentials.ApiToken(
                    username = server.username ?: "root",
                    realm = server.realm,
                    tokenId = server.tokenId ?: "",
                    tokenSecret = tokenSecret,
                )
                when (val result = login(serverId, creds)) {
                    is ApiResult.Failure -> {
                        _state.update { it.copy(isLoading = false, error = result.error) }
                        return@launch
                    }
                    is ApiResult.Success -> Unit
                }
            }

            val clusterResult = getCluster(serverId)
            val guestsResult = listGuests(serverId)
            _state.update {
                it.copy(
                    isLoading = false,
                    cluster = (clusterResult as? ApiResult.Success)?.value,
                    guests = (guestsResult as? ApiResult.Success)?.value ?: emptyList(),
                    error = (clusterResult as? ApiResult.Failure)?.error
                        ?: (guestsResult as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
