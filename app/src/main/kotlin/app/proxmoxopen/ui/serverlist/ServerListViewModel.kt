package app.proxmoxopen.ui.serverlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.proxmoxopen.domain.model.Server
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.domain.usecase.ListServersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServerListUiState(
    val servers: List<Server> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ServerListViewModel @Inject constructor(
    listServers: ListServersUseCase,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    val uiState: StateFlow<ServerListUiState> =
        listServers()
            .map { ServerListUiState(servers = it, isLoading = false) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT),
                ServerListUiState(),
            )

    fun delete(server: Server) {
        viewModelScope.launch { serverRepository.delete(server) }
    }

    companion object {
        private const val STOP_TIMEOUT = 5_000L
    }
}
