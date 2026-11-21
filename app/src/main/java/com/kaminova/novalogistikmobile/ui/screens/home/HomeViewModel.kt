package com.kaminova.novalogistikmobile.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaminova.novalogistikmobile.data.api.DriverDashboardData
import com.kaminova.novalogistikmobile.data.repository.DriverRepository
import com.kaminova.novalogistikmobile.data.session.SessionManager
import com.kaminova.novalogistikmobile.data.session.StoredSession
import com.kaminova.novalogistikmobile.util.Formatters
import com.kaminova.novalogistikmobile.util.Haversine
import com.kaminova.novalogistikmobile.util.LocationFix
import com.kaminova.novalogistikmobile.util.LocationWatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val data: DriverDashboardData? = null,
    val error: String? = null,
    val liveFix: LocationFix? = null,
    val distanceToNextMeters: Long? = null,
    val showTripDetails: Boolean = false,
    val session: StoredSession? = null
)

class HomeViewModel(
    private val driverRepository: DriverRepository,
    private val sessionManager: SessionManager,
    context: Context
) : ViewModel() {

    private val locationWatcher = LocationWatcher(context)
    private var locationJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(session = sessionManager.sessionState.value)
        viewModelScope.launch {
            sessionManager.sessionState.collect { session ->
                _uiState.value = _uiState.value.copy(session = session)
            }
        }
        loadDashboard()
    }

    fun startLocationWatch() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            runCatching {
                locationWatcher.watchLocation(intervalMs = 1000).collect { fix ->
                    val next = _uiState.value.data?.current?.nextCheckpoint
                    val dist = if (next != null) {
                        Haversine.calculateMeters(
                            fix.latitude, fix.longitude,
                            next.latitude, next.longitude
                        )
                    } else null
                    _uiState.value = _uiState.value.copy(
                        liveFix = fix,
                        distanceToNextMeters = dist
                    )
                }
            }
        }
    }

    fun stopLocationWatch() {
        locationJob?.cancel()
        locationJob = null
    }

    fun toggleTripDetails() {
        _uiState.value = _uiState.value.copy(showTripDetails = !_uiState.value.showTripDetails)
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        loadDashboard(isRefresh = true)
    }

    fun loadDashboard(isRefresh: Boolean = false) {
        val today = LocalDate.now().toString()
        viewModelScope.launch {
            val result = driverRepository.getDashboard(from = today, to = today)
            result.fold(
                onSuccess = { dashboard ->
                    val fix = _uiState.value.liveFix
                    val next = dashboard.current?.nextCheckpoint
                    val dist = if (fix != null && next != null) {
                        Haversine.calculateMeters(fix.latitude, fix.longitude, next.latitude, next.longitude)
                    } else null

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        data = dashboard,
                        distanceToNextMeters = dist,
                        error = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = err.message ?: "Gagal memuat dashboard."
                    )
                }
            )
        }
    }
}
