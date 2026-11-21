package com.kaminova.novalogistikmobile.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaminova.novalogistikmobile.data.api.MobileConfig
import com.kaminova.novalogistikmobile.data.repository.AuthRepository
import com.kaminova.novalogistikmobile.data.repository.DriverRepository
import com.kaminova.novalogistikmobile.data.session.SessionManager
import com.kaminova.novalogistikmobile.data.session.StoredSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class ProfileUiState(
    val session: StoredSession? = null,
    val selectedTimeZone: String = "Asia/Jakarta",
    val config: MobileConfig? = null,
    val clockSkewSeconds: Long? = null,
    val isLoadingConfig: Boolean = true,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false,
    val configError: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val driverRepository: DriverRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            session = sessionManager.sessionState.value,
            selectedTimeZone = sessionManager.timeZoneState.value
        )
        loadConfig()
    }

    val usePhoneColors = sessionManager.usePhoneColors

    fun setUsePhoneColors(enabled: Boolean) = sessionManager.setUsePhoneColors(enabled)

    fun onTimeZoneSelected(zoneId: String) {
        sessionManager.saveTimeZone(zoneId)
        _uiState.value = _uiState.value.copy(selectedTimeZone = zoneId)
    }

    fun loadConfig() {
        _uiState.value = _uiState.value.copy(isLoadingConfig = true, configError = null)
        viewModelScope.launch {
            val result = driverRepository.getMobileConfig()
            result.fold(
                onSuccess = { cfg ->
                    val skew = runCatching {
                        val serverEpoch = Instant.parse(cfg.serverTime).epochSecond
                        val deviceEpoch = Instant.now().epochSecond
                        deviceEpoch - serverEpoch
                    }.getOrNull()

                    _uiState.value = _uiState.value.copy(
                        config = cfg,
                        clockSkewSeconds = skew,
                        isLoadingConfig = false,
                        configError = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        configError = err.message
                    )
                }
            )
        }
    }

    fun logout() {
        if (_uiState.value.isLoggingOut) return
        _uiState.value = _uiState.value.copy(isLoggingOut = true)
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isLoggingOut = false, isLoggedOut = true)
        }
    }
}
