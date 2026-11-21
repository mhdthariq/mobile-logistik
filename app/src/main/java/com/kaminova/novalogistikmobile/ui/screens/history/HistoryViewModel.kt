package com.kaminova.novalogistikmobile.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaminova.novalogistikmobile.data.api.TransportListItem
import com.kaminova.novalogistikmobile.data.repository.DriverRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val rawList: List<TransportListItem> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
)

class HistoryViewModel(
    private val driverRepository: DriverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val filteredList: StateFlow<List<TransportListItem>> = _uiState
        .combine(_uiState) { state, _ ->
            val needle = state.searchQuery.trim().lowercase()
            if (needle.isBlank()) {
                state.rawList
            } else {
                state.rawList.filter { item ->
                    val haystack = listOf(
                        item.transportCode,
                        item.routeName ?: "",
                        item.vehicleNumber,
                        item.origin ?: "",
                        item.destination ?: ""
                    )
                    haystack.any { it.lowercase().contains(needle) }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadHistory()
    }

    fun onSearchChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        loadHistory(isRefresh = true)
    }

    fun loadHistory(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val result = driverRepository.getTransportHistory()
            result.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        rawList = list,
                        error = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = err.message ?: "Gagal memuat riwayat transport."
                    )
                }
            )
        }
    }
}
