package com.kaminova.novalogistikmobile.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaminova.novalogistikmobile.data.api.EvidenceCapture
import com.kaminova.novalogistikmobile.data.api.ExpenseSummary
import com.kaminova.novalogistikmobile.data.api.NetworkException
import com.kaminova.novalogistikmobile.data.api.TransportDetail
import com.kaminova.novalogistikmobile.data.api.TransportExpenseRow
import com.kaminova.novalogistikmobile.data.repository.DriverRepository
import com.kaminova.novalogistikmobile.data.repository.ExpenseRepository
import com.kaminova.novalogistikmobile.data.session.CaptureManager
import com.kaminova.novalogistikmobile.data.session.CaptureRequest
import com.kaminova.novalogistikmobile.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class ExpenseReceipt(
    val dataUrl: String,
    val capture: EvidenceCapture?
)

data class ExpensesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val transport: TransportDetail? = null,
    val rows: List<TransportExpenseRow> = emptyList(),
    val summary: ExpenseSummary? = null,
    val selectedType: String = "BBM",
    val amountText: String = "",
    val description: String = "",
    val receipts: List<ExpenseReceipt> = emptyList(),
    val isFormOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val notice: String? = null,
    val formError: String? = null,
    val error: String? = null,
    val timeZone: String = "Asia/Jakarta"
)

class ExpensesViewModel(
    val transportId: Long,
    private val expenseRepository: ExpenseRepository,
    private val driverRepository: DriverRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(timeZone = sessionManager.timeZoneState.value)
        loadData()
    }

    val canCreate: Boolean
        get() = sessionManager.sessionState.value?.user?.permissions.orEmpty().any {
            it == "*" || it == "transport.expense.create"
        }

    suspend fun receiptPhotos(expenseId: Long) = expenseRepository.getExpensePhotos(transportId, expenseId)

    val rowCapture: EvidenceCapture?
        get() = _uiState.value.receipts.find { it.capture != null }?.capture
            ?: _uiState.value.receipts.firstOrNull()?.capture

    fun openForm() {
        _uiState.value = _uiState.value.copy(isFormOpen = true)
    }

    fun closeForm() {
        if (!_uiState.value.isSubmitting) {
            _uiState.value = _uiState.value.copy(isFormOpen = false)
        }
    }

    fun onTypeSelect(type: String) {
        _uiState.value = _uiState.value.copy(selectedType = type, formError = null)
    }

    fun onAmountChange(amount: String) {
        val filtered = amount.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(amountText = filtered, formError = null)
    }

    fun onDescriptionChange(desc: String) {
        _uiState.value = _uiState.value.copy(description = desc, formError = null)
    }

    fun removeReceipt(index: Int) {
        val updated = _uiState.value.receipts.toMutableList().apply { removeAt(index) }
        _uiState.value = _uiState.value.copy(receipts = updated, formError = null)
    }

    fun startAddReceipt(onNavigateToCapture: () -> Unit) {
        if (_uiState.value.receipts.size >= 8) {
            _uiState.value = _uiState.value.copy(formError = "Maksimal 8 foto struk per biaya.")
            return
        }

        val typeLabel = EXPENSE_OPTIONS.find { it.first == _uiState.value.selectedType }?.second ?: _uiState.value.selectedType
        val transport = _uiState.value.transport
        val request = CaptureRequest(
            title = "Foto Struk · $typeLabel",
            requireGps = false,
            contextLines = listOf(transport?.transportCode ?: "#$transportId"),
            vehicle = transport?.vehicle,
            driver = transport?.driver
        )

        viewModelScope.launch {
            val pending = CaptureManager.requestCapture(request)
            onNavigateToCapture()
            val capture = pending.await()
            if (capture != null) {
                val nextList = _uiState.value.receipts + ExpenseReceipt(capture.photoDataUrl, capture)
                _uiState.value = _uiState.value.copy(receipts = nextList, formError = null)
            }
        }
    }

    fun submitExpense() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val amount = state.amountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _uiState.value = state.copy(formError = "Nominal biaya harus angka lebih dari nol.")
            return
        }
        if (state.receipts.isEmpty()) {
            _uiState.value = state.copy(formError = "Sertakan minimal satu foto struk sebagai bukti.")
            return
        }

        val capture = rowCapture
        if (capture != null) {
            val age = (Instant.now().epochSecond - runCatching { Instant.parse(capture.capturedAt).epochSecond }.getOrDefault(0L))
            if (age > 300) {
                _uiState.value = state.copy(
                    formError = "Foto sudah lebih dari 5 menit. Ambil ulang struk."
                )
                return
            }
        }

        _uiState.value = state.copy(isSubmitting = true, formError = null, notice = null)

        viewModelScope.launch {
            val result = expenseRepository.createExpense(
                transportId = transportId,
                type = state.selectedType,
                amount = amount,
                description = state.description.trim().ifBlank { null },
                photos = state.receipts.map { it.dataUrl },
                capture = capture
            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        isFormOpen = false,
                        amountText = "",
                        description = "",
                        receipts = emptyList(),
                        notice = "Biaya tersimpan (menunggu persetujuan).",
                        formError = null
                    )
                    loadData()
                },
                onFailure = { err ->
                    val netErr = err as? NetworkException
                    if (netErr?.isAmbiguous == true) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            formError = "${netErr.message} — respons tidak pasti: daftar biaya diperbarui; periksa apakah biaya sudah tersimpan sebelum mengirim ulang."
                        )
                        loadData()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            formError = netErr?.message ?: err.message ?: "Gagal menyimpan biaya."
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        loadData(isRefresh = true)
    }

    fun loadData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val tDetail = driverRepository.getTransportDetail(transportId).getOrNull()
            val expResult = expenseRepository.getExpenses(transportId)

            expResult.fold(
                onSuccess = { (rows, summary) ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        transport = tDetail ?: _uiState.value.transport,
                        rows = rows,
                        summary = summary,
                        error = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = err.message ?: "Gagal memuat data biaya operasional."
                    )
                }
            )
        }
    }

    companion object {
        val EXPENSE_OPTIONS = listOf(
            "BBM" to "Biaya BBM",
            "TOL" to "Biaya Tol",
            "PARKIR" to "Biaya Parkir",
            "BONGKAR" to "Biaya Bongkar",
            "ANGKUT" to "Biaya Angkut",
            "MAKAN" to "Uang Makan"
        )
    }
}
