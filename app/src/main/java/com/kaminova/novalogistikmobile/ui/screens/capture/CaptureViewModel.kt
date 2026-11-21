package com.kaminova.novalogistikmobile.ui.screens.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaminova.novalogistikmobile.data.api.EvidenceCapture
import com.kaminova.novalogistikmobile.data.session.CaptureManager
import com.kaminova.novalogistikmobile.data.session.CaptureRequest
import com.kaminova.novalogistikmobile.data.session.SessionManager
import com.kaminova.novalogistikmobile.util.EvidenceStamp
import com.kaminova.novalogistikmobile.util.EvidenceStampData
import com.kaminova.novalogistikmobile.util.EvidenceZones
import com.kaminova.novalogistikmobile.util.ImageCompressor
import com.kaminova.novalogistikmobile.util.LocationFix
import com.kaminova.novalogistikmobile.util.LocationWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.Locale
import kotlin.math.abs

enum class CaptureStep {
    PERMISSIONS,
    LIVE,
    PROCESSING,
    PREVIEW,
    BLOCKED
}

data class FrozenData(
    val photoDataUrl: String,
    val previewBitmap: Bitmap,
    val fix: LocationFix?,
    val capturedAt: Instant,
    val address: String?,
    val timeZone: String
)

data class CaptureUiState(
    val step: CaptureStep = CaptureStep.PERMISSIONS,
    val request: CaptureRequest? = null,
    val company: String = "Nova Logistik",
    val timeZone: String = EvidenceZones.DEFAULT_ZONE_ID,
    val liveFix: LocationFix? = null,
    val address: String? = null,
    val flashEnabled: Boolean = false,
    val frozen: FrozenData? = null,
    val error: String? = null,
    val blockReason: String? = null
)

class CaptureViewModel(
    private val sessionManager: SessionManager,
    private val context: Context
) : ViewModel() {

    private val locationWatcher = LocationWatcher(context)
    private var locationJob: Job? = null
    private var geocoderJob: Job? = null
    private val geocoder = Geocoder(context, Locale("id", "ID"))

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            request = CaptureManager.currentRequest.value,
            company = sessionManager.sessionState.value?.companyName ?: "Nova Logistik",
            timeZone = sessionManager.timeZoneState.value
        )
    }

    fun onPermissionsResult(hasCamera: Boolean, hasLocation: Boolean) {
        val req = _uiState.value.request
        if (!hasCamera) {
            _uiState.value = _uiState.value.copy(
                step = CaptureStep.BLOCKED,
                blockReason = "Izin kamera diperlukan untuk mengambil foto bukti. Aktifkan izin kamera di pengaturan ponsel Anda."
            )
            return
        }
        if (!hasLocation && req?.requireGps == true) {
            _uiState.value = _uiState.value.copy(
                step = CaptureStep.BLOCKED,
                blockReason = "Bukti ini membutuhkan lokasi GPS yang presisi. Aktifkan izin lokasi di pengaturan ponsel Anda."
            )
            return
        }

        _uiState.value = _uiState.value.copy(step = CaptureStep.LIVE, blockReason = null)
        startLocationWatch()
    }

    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(flashEnabled = !_uiState.value.flashEnabled)
    }

    private fun startLocationWatch() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            runCatching {
                locationWatcher.watchLocation(intervalMs = 1000).collect { fix ->
                    _uiState.value = _uiState.value.copy(liveFix = fix)
                    reverseGeocode(fix.latitude, fix.longitude)
                }
            }
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        if (geocoderJob?.isActive == true) return
        geocoderJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        val label = formatAddress(addresses.firstOrNull())
                        _uiState.value = _uiState.value.copy(address = label)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    val label = formatAddress(addresses?.firstOrNull())
                    _uiState.value = _uiState.value.copy(address = label)
                }
            }
        }
    }

    private fun formatAddress(addr: Address?): String? {
        if (addr == null) return null
        val parts = listOfNotNull(
            addr.featureName,
            addr.thoroughfare,
            addr.subLocality,
            addr.locality,
            addr.subAdminArea,
            addr.adminArea,
            addr.postalCode
        ).distinct()
        val text = parts.joinToString(", ")
        return if (text.length > 240) text.take(239) + "…" else text
    }

    internal fun beginCapture(): EvidenceStampData? {
        val state = _uiState.value
        val request = state.request ?: return null
        if (state.step != CaptureStep.LIVE || request.requireGps && state.liveFix == null) return null
        _uiState.value = state.copy(step = CaptureStep.PROCESSING, error = null)
        return EvidenceStampData(state.company, request.title, Instant.now(), state.timeZone, state.liveFix,
            state.address, request.contextLines, request.vehicle, request.driver)
    }

    internal fun processCapturedPhoto(file: File, stamp: EvidenceStampData) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val renderer = EvidenceStamp(context)
                val compressed = ImageCompressor.compressImageFile(file) {
                    renderer.draw(Canvas(it), it.width.toFloat(), it.height.toFloat(), stamp)
                }
                file.delete() // Clean up raw camera file

                _uiState.value = _uiState.value.copy(
                    step = CaptureStep.PREVIEW,
                    frozen = FrozenData(
                        photoDataUrl = compressed.dataUrl,
                        previewBitmap = compressed.bitmap,
                        fix = stamp.fix,
                        capturedAt = stamp.capturedAt,
                        address = stamp.address,
                        timeZone = stamp.timeZone
                    ),
                    error = null
                )
            } catch (e: Throwable) {
                file.delete()
                _uiState.value = _uiState.value.copy(
                    step = CaptureStep.LIVE,
                    error = e.message ?: "Gagal memproses foto. Silakan ambil ulang."
                )
            }
        }
    }

    fun retake(error: String? = null) {
        _uiState.value = _uiState.value.copy(
            step = CaptureStep.LIVE,
            frozen = null,
            error = error
        )
    }

    fun usePhoto(onComplete: () -> Unit) {
        val frozen = _uiState.value.frozen ?: return
        val req = _uiState.value.request

        if (req?.requireGps == true && frozen.fix == null) {
            _uiState.value = _uiState.value.copy(
                error = "Bukti ini membutuhkan koordinat GPS. Tunggu indikator GPS menampilkan posisi, lalu ambil ulang foto."
            )
            return
        }

        // Validate freshness (server rules: <= 300s old, <= 30s future)
        val ageSeconds = (Instant.now().epochSecond - frozen.capturedAt.epochSecond)
        if (ageSeconds > 300) {
            _uiState.value = _uiState.value.copy(
                error = "Waktu ambil foto sudah lebih dari 5 menit — ambil ulang foto."
            )
            return
        }
        if (ageSeconds < -30) {
            _uiState.value = _uiState.value.copy(
                error = "Jam perangkat terlalu maju — periksa waktu di pengaturan ponsel Anda."
            )
            return
        }

        val result = EvidenceCapture(
            photoDataUrl = frozen.photoDataUrl,
            latitude = frozen.fix?.latitude,
            longitude = frozen.fix?.longitude,
            locationAccuracy = frozen.fix?.accuracy,
            capturedAt = frozen.capturedAt.toString(),
            locationLabel = frozen.address?.trim()?.ifBlank { null },
            timeZone = frozen.timeZone,
            mockLocation = frozen.fix?.isMock
        )

        CaptureManager.completeCapture(result)
        onComplete()
    }

    fun cancel(onComplete: () -> Unit) {
        CaptureManager.completeCapture(null)
        onComplete()
    }

    override fun onCleared() {
        super.onCleared()
        locationJob?.cancel()
        geocoderJob?.cancel()
    }
}
