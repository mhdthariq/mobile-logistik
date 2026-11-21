package com.kaminova.novalogistikmobile.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaminova.novalogistikmobile.data.api.CheckinResponse
import com.kaminova.novalogistikmobile.data.api.Checkpoint
import com.kaminova.novalogistikmobile.data.api.EvidenceCapture
import com.kaminova.novalogistikmobile.data.api.NetworkException
import com.kaminova.novalogistikmobile.data.api.TransportDetail
import com.kaminova.novalogistikmobile.data.repository.DriverRepository
import com.kaminova.novalogistikmobile.data.session.CaptureManager
import com.kaminova.novalogistikmobile.data.session.CaptureRequest
import com.kaminova.novalogistikmobile.data.session.SessionManager
import com.kaminova.novalogistikmobile.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransportDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val transport: TransportDetail? = null,
    val error: String? = null,
    val actionNotice: String? = null,
    val actionError: String? = null,
    val busyAction: String? = null
)

class TransportDetailViewModel(
    val transportId: Long,
    private val driverRepository: DriverRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransportDetailUiState())
    val uiState: StateFlow<TransportDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    val permissions: List<String>
        get() = sessionManager.sessionState.value?.user?.permissions ?: emptyList()

    fun hasPermission(slug: String): Boolean {
        return permissions.contains("*") || permissions.contains(slug)
    }

    val canDepart: Boolean get() = hasPermission("transport.depart")
    val canCheckin: Boolean get() = hasPermission("transport.checkin")
    val canArrive: Boolean get() = hasPermission("transport.arrive")
    val canArriveWithoutPhoto: Boolean get() = hasPermission("transport.arrive_without_photo")

    fun getCheckedInIds(): Set<Long> {
        val records = _uiState.value.transport?.checkpointRecords ?: emptyList()
        return records.map { it.checkpointId }.toSet()
    }

    fun getNextCheckpoint(): Checkpoint? {
        val transport = _uiState.value.transport ?: return null
        val checkedIn = getCheckedInIds()
        return transport.checkpoints.firstOrNull { !checkedIn.contains(it.id) }
    }

    fun isFirstCheckpointCheckedIn(): Boolean {
        val transport = _uiState.value.transport ?: return false
        val first = transport.checkpoints.firstOrNull() ?: return true
        return getCheckedInIds().contains(first.id)
    }

    private fun getTripContext(t: TransportDetail): List<String> {
        return listOf(
            t.transportCode,
            "${t.origin ?: "?"} → ${t.destination ?: "?"}",
            "${t.shipmentCount} kiriman"
        )
    }

    suspend fun proofPhotos(transport: TransportDetail): Result<List<String>> =
        if (transport.trackingMode == "ACTIVITY") {
            driverRepository.getActivities(transportId).map { activities ->
                activities.map { it.photoUrl }.filter(String::isNotBlank)
            }
        } else {
            Result.success(transport.checkpointRecords.mapNotNull { it.photoUrl?.takeIf(String::isNotBlank) })
        }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true, actionError = null)
        loadDetail(isRefresh = true)
    }

    fun loadDetail(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val result = driverRepository.getTransportDetail(transportId)
            result.fold(
                onSuccess = { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        transport = detail,
                        error = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = err.message ?: "Gagal memuat detail transport."
                    )
                }
            )
        }
    }

    fun depart() {
        _uiState.value = _uiState.value.copy(busyAction = "depart", actionError = null, actionNotice = null)
        viewModelScope.launch {
            val result = driverRepository.departTransport(transportId)
            result.fold(
                onSuccess = { update ->
                    _uiState.value = _uiState.value.copy(
                        busyAction = null,
                        transport = _uiState.value.transport?.let(update::applyTo),
                        actionNotice = "Transport sekarang BERANGKAT. Selamat jalan — jaga keselamatan!"
                    )
                    loadDetail()
                },
                onFailure = { err ->
                    handleMutationError(err)
                }
            )
        }
    }

    fun startCheckin(onNavigateToCapture: () -> Unit) {
        val transport = _uiState.value.transport ?: return
        val next = getNextCheckpoint() ?: return

        val request = CaptureRequest(
            title = "Check-in ${next.name}",
            requireGps = true,
            preferFrontCamera = true,
            contextLines = getTripContext(transport),
            vehicle = transport.vehicle,
            driver = transport.driver
        )

        viewModelScope.launch {
            val pending = CaptureManager.requestCapture(request)
            onNavigateToCapture()
            val capture = pending.await()
            if (capture != null) {
                performCheckin(next.id, capture)
            }
        }
    }

    private fun performCheckin(checkpointId: Long, capture: EvidenceCapture) {
        _uiState.value = _uiState.value.copy(busyAction = "checkin", actionError = null, actionNotice = null)
        viewModelScope.launch {
            val result = driverRepository.checkinAtCheckpoint(transportId, checkpointId, capture)
            result.fold(
                onSuccess = { checkinResponse ->
                    _uiState.value = _uiState.value.copy(
                        busyAction = null,
                        actionNotice = checkinResponse.message
                    )
                    loadDetail()
                },
                onFailure = { err ->
                    handleMutationError(err)
                }
            )
        }
    }

    fun startReportActivity(onNavigateToCapture: () -> Unit) {
        val transport = _uiState.value.transport ?: return
        val request = CaptureRequest(
            title = "Laporan Aktivitas",
            requireGps = true,
            contextLines = getTripContext(transport),
            vehicle = transport.vehicle,
            driver = transport.driver
        )

        viewModelScope.launch {
            val pending = CaptureManager.requestCapture(request)
            onNavigateToCapture()
            val capture = pending.await()
            if (capture != null) {
                performActivityReport(capture)
            }
        }
    }

    private fun performActivityReport(capture: EvidenceCapture) {
        _uiState.value = _uiState.value.copy(busyAction = "activity", actionError = null, actionNotice = null)
        viewModelScope.launch {
            val result = driverRepository.reportActivity(transportId, capture)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        busyAction = null,
                        actionNotice = "Laporan aktivitas terkirim."
                    )
                    loadDetail()
                },
                onFailure = { err ->
                    handleMutationError(err)
                }
            )
        }
    }

    fun arrive(capture: EvidenceCapture? = null) {
        _uiState.value = _uiState.value.copy(busyAction = "arrive", actionError = null, actionNotice = null)
        viewModelScope.launch {
            val result = driverRepository.arriveTransport(transportId, capture)
            result.fold(
                onSuccess = { update ->
                    _uiState.value = _uiState.value.copy(
                        busyAction = null,
                        transport = _uiState.value.transport?.let(update::applyTo),
                        actionNotice = "Transport ditandai TIBA. Terima kasih!"
                    )
                    loadDetail()
                },
                onFailure = { err ->
                    handleMutationError(err)
                }
            )
        }
    }

    fun startArriveWithPhoto(onNavigateToCapture: () -> Unit) {
        val transport = _uiState.value.transport ?: return
        val request = CaptureRequest(
            title = "Bukti Tiba di Tujuan",
            requireGps = true,
            contextLines = getTripContext(transport),
            vehicle = transport.vehicle,
            driver = transport.driver
        )

        viewModelScope.launch {
            val pending = CaptureManager.requestCapture(request)
            onNavigateToCapture()
            val capture = pending.await()
            if (capture != null) {
                arrive(capture)
            }
        }
    }

    private fun handleMutationError(err: Throwable) {
        val netErr = err as? NetworkException
        if (netErr?.isAmbiguous == true) {
            _uiState.value = _uiState.value.copy(
                busyAction = null,
                actionError = "${netErr.message} — respons tidak pasti: muat ulang halaman ini dan periksa status terbaru sebelum mencoba lagi (tidak ada retry otomatis)."
            )
            loadDetail()
        } else {
            _uiState.value = _uiState.value.copy(
                busyAction = null,
                actionError = netErr?.message ?: err.message ?: "Aksi gagal diproses."
            )
        }
    }
}
