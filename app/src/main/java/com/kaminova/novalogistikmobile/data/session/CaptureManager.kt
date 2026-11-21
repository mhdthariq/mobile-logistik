package com.kaminova.novalogistikmobile.data.session

import com.kaminova.novalogistikmobile.data.api.EvidenceCapture
import com.kaminova.novalogistikmobile.data.api.VehicleDetail
import com.kaminova.novalogistikmobile.data.api.UserSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CaptureRequest(
    val title: String,
    val requireGps: Boolean = true,
    val contextLines: List<String> = emptyList(),
    val preferFrontCamera: Boolean = false,
    val vehicle: VehicleDetail? = null,
    val driver: UserSummary? = null
)

object CaptureManager {
    private var pendingDeferred: CompletableDeferred<EvidenceCapture?>? = null

    private val _currentRequest = MutableStateFlow<CaptureRequest?>(null)
    val currentRequest: StateFlow<CaptureRequest?> = _currentRequest.asStateFlow()

    fun requestCapture(request: CaptureRequest): CompletableDeferred<EvidenceCapture?> {
        // Cancel any existing stale request
        pendingDeferred?.complete(null)

        val deferred = CompletableDeferred<EvidenceCapture?>()
        pendingDeferred = deferred
        _currentRequest.value = request
        return deferred
    }

    fun completeCapture(capture: EvidenceCapture?) {
        pendingDeferred?.complete(capture)
        pendingDeferred = null
        _currentRequest.value = null
    }
}
