package com.kaminova.novalogistikmobile.ui.screens.drops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaminova.novalogistikmobile.data.api.AuthUser
import com.kaminova.novalogistikmobile.data.api.DropShipment
import com.kaminova.novalogistikmobile.data.api.DropsBoard
import com.kaminova.novalogistikmobile.data.api.NetworkException
import com.kaminova.novalogistikmobile.data.api.TransportDetail
import com.kaminova.novalogistikmobile.data.repository.DriverRepository
import com.kaminova.novalogistikmobile.data.repository.ExpenseRepository
import com.kaminova.novalogistikmobile.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DropsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val transport: TransportDetail? = null,
    val board: DropsBoard? = null,
    val busyShipmentId: Long? = null,
    val notice: String? = null,
    val actionError: String? = null,
    val error: String? = null
)

internal fun DropsUiState.canUnload(shipment: DropShipment, actor: AuthUser?): Boolean {
    val board = board ?: return false
    if (board.transportMode != "MULTI_DROP" || board.transportStatus != "DEPARTED" || actor == null) return false

    val isCrew = actor.employeeId?.let { it == transport?.driver?.id || it == transport?.kenek?.id } == true
    if (!isCrew && !actor.isOwner && "*" !in actor.permissions && "transport.arrive" !in actor.permissions) return false

    if (board.trackingMode == "ACTIVITY") {
        return shipment.dropStatus == "LOADED" || shipment.dropStatus == "AT_DROP_POINT"
    }

    return shipment.dropStatus == "AT_DROP_POINT" &&
        board.checkpoints.any { it.id == shipment.effectiveDropCheckpointId && it.checkedIn }
}

class DropsViewModel(
    val transportId: Long,
    private val expenseRepository: ExpenseRepository,
    private val driverRepository: DriverRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DropsUiState())
    val uiState: StateFlow<DropsUiState> = _uiState.asStateFlow()

    init {
        loadBoard()
    }

    fun canUnload(shipment: DropShipment): Boolean =
        _uiState.value.canUnload(shipment, sessionManager.sessionState.value?.user)

    fun dropShipment(shipment: DropShipment) {
        val state = _uiState.value
        val current = state.board?.shipments?.firstOrNull { it.shipmentId == shipment.shipmentId } ?: return
        if (state.busyShipmentId != null || state.isRefreshing || state.error != null || !canUnload(current)) return

        _uiState.value = _uiState.value.copy(
            busyShipmentId = shipment.shipmentId,
            actionError = null,
            notice = null
        )

        viewModelScope.launch {
            val result = expenseRepository.dropShipment(transportId, shipment.shipmentId)
            result.fold(
                onSuccess = { res ->
                    _uiState.value = _uiState.value.copy(
                        notice = "${shipment.masterCode} diturunkan${if (!res.checkpointName.isNullOrBlank()) " di ${res.checkpointName}" else ""} — menunggu persetujuan delivery."
                    )
                    updateBoard()
                },
                onFailure = { err ->
                    val netErr = err as? NetworkException
                    if (netErr?.isAmbiguous == true) {
                        _uiState.value = _uiState.value.copy(
                            actionError = "${netErr.message} — respons tidak pasti: papan diperbarui; periksa status resi sebelum mencoba lagi."
                        )
                        updateBoard()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            busyShipmentId = null,
                            actionError = netErr?.message ?: err.message ?: "Gagal menurunkan resi."
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        val state = _uiState.value
        if (state.busyShipmentId != null || state.isRefreshing) return
        _uiState.value = state.copy(isRefreshing = true, actionError = null)
        loadBoard()
    }

    fun loadBoard() {
        viewModelScope.launch { updateBoard() }
    }

    private suspend fun updateBoard() {
        val tDetail = driverRepository.getTransportDetail(transportId).getOrNull()
        val boardResult = expenseRepository.getDropsBoard(transportId)

        boardResult.fold(
            onSuccess = { board ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    busyShipmentId = null,
                    transport = tDetail ?: _uiState.value.transport,
                    board = board,
                    error = null
                )
            },
            onFailure = { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    busyShipmentId = null,
                    error = err.message ?: "Gagal memuat papan multi-drop."
                )
            }
        )
    }
}
