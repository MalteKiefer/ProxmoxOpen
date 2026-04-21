package de.kiefer_networks.proxmoxopen.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kiefer_networks.proxmoxopen.domain.model.SearchResult
import de.kiefer_networks.proxmoxopen.domain.repository.SearchRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: ApiError? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchRepository: SearchRepository,
) : ViewModel() {

    val serverId: Long = savedStateHandle.toRoute<Route.Search>().serverId

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _queryFlow
                .debounce(250)
                .distinctUntilChanged()
                .collect { query -> runSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        _queryFlow.value = query
    }

    private suspend fun runSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(results = emptyList(), isLoading = false, error = null) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = searchRepository.search(serverId, trimmed)) {
            is ApiResult.Success -> _state.update {
                it.copy(results = result.value, isLoading = false, error = null)
            }
            is ApiResult.Failure -> _state.update {
                it.copy(results = emptyList(), isLoading = false, error = result.error)
            }
        }
    }
}
