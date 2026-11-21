package com.kaminova.novalogistikmobile.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.kaminova.novalogistikmobile.ui.components.ExpressiveRefreshBox
import com.kaminova.novalogistikmobile.ui.components.EvidenceImage
import com.kaminova.novalogistikmobile.ui.components.ErrorBanner
import com.kaminova.novalogistikmobile.ui.components.ErrorView
import com.kaminova.novalogistikmobile.ui.components.LabeledRow
import com.kaminova.novalogistikmobile.ui.components.LoadingView
import com.kaminova.novalogistikmobile.ui.components.PrimaryButton
import com.kaminova.novalogistikmobile.ui.components.ProofPhotos
import com.kaminova.novalogistikmobile.ui.components.ScreenHeading
import com.kaminova.novalogistikmobile.ui.components.SectionCard
import com.kaminova.novalogistikmobile.ui.components.StatusBadge
import com.kaminova.novalogistikmobile.ui.components.SuccessBanner
import com.kaminova.novalogistikmobile.util.EvidenceZones
import com.kaminova.novalogistikmobile.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCapture: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var receiptId by rememberSaveable { mutableStateOf<Long?>(null) }
    receiptId?.let { id ->
        val result by produceState<Result<List<String>>?>(null, id) { value = viewModel.receiptPhotos(id) }
        ProofPhotos("Bukti biaya", result) { receiptId = null }
    }

    if (uiState.isLoading && uiState.rows.isEmpty()) {
        LoadingView(label = "Memuat biaya operasional…")
        return
    }

    if (uiState.error != null && uiState.rows.isEmpty()) {
        ErrorView(message = uiState.error!!, onRetry = { viewModel.loadData() })
        return
    }

    val transportCode = uiState.transport?.transportCode ?: "#${viewModel.transportId}"
    val summary = uiState.summary
    if (uiState.isFormOpen && viewModel.canCreate) {
        ExpenseFormDialog(uiState, viewModel, onNavigateToCapture)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biaya Operasional", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        ExpressiveRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ScreenHeading(
                    eyebrow = transportCode,
                    title = "Biaya perjalanan",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong
                )

                if (uiState.notice != null) {
                    SuccessBanner(message = uiState.notice!!)
                }

                // Summary Card
                if (summary != null) {
                    SectionCard(title = "Ringkasan") {
                        LabeledRow(label = "Total disetujui", value = Formatters.formatRupiah(summary.total))
                        ExpensesViewModel.EXPENSE_OPTIONS.forEach { (typeVal, typeLabel) ->
                            val byType = summary.byType[typeVal]
                            if (byType != null && (byType.count > 0 || byType.total > 0)) {
                                LabeledRow(
                                    label = "$typeLabel (${byType.count}×)",
                                    value = Formatters.formatRupiah(byType.total)
                                )
                            }
                        }
                    }
                }

                if (viewModel.canCreate) {
                    PrimaryButton(label = "Catat biaya", onClick = viewModel::openForm)
                }

                // History Section
                SectionCard(title = "Riwayat Biaya (${uiState.rows.size})") {
                    if (uiState.rows.isEmpty()) {
                        Text(
                            text = "Belum ada biaya operasional pada transport ini.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.rows.forEach { row ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
                                        .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.small)
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val typeLabel = ExpensesViewModel.EXPENSE_OPTIONS.find { it.first == row.type }?.second ?: row.type
                                            SelectionContainer {
                                                Text(
                                                    text = "${row.expenseCode} · $typeLabel",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            StatusBadge(status = row.status)
                                        }
                                        Text(
                                            text = Formatters.formatRupiah(row.amount),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (!row.description.isNullOrBlank()) {
                                            Text(
                                                text = row.description,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            text = Formatters.formatDateTime(row.createdAt),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                        if (row.hasPhoto || row.photoCount > 0) {
                                            TextButton(onClick = { receiptId = row.id }) {
                                                Text(if (row.photoCount > 1) "Lihat bukti (${row.photoCount})" else "Lihat bukti")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpenseFormDialog(
    uiState: ExpensesUiState,
    viewModel: ExpensesViewModel,
    onNavigateToCapture: () -> Unit
) {
    val rowCapture = viewModel.rowCapture
    AlertDialog(
        onDismissRequest = viewModel::closeForm,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Catat biaya baru")
                uiState.formError?.let { ErrorBanner(message = it) }
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !uiState.isSubmitting,
            dismissOnClickOutside = false
        ),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Type picker chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Jenis biaya",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpensesViewModel.EXPENSE_OPTIONS.forEach { (typeVal, typeLabel) ->
                            FilterChip(
                                selected = uiState.selectedType == typeVal,
                                onClick = { viewModel.onTypeSelect(typeVal) },
                                label = { Text(typeLabel) },
                                enabled = !uiState.isSubmitting,
                                shapes = FilterChipDefaults.shapes()
                            )
                        }
                    }
                }

                // Nominal Input
                OutlinedTextField(
                    value = uiState.amountText,
                    enabled = !uiState.isSubmitting,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Nominal (Rp)") },
                    placeholder = { Text("mis. 150000") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Description Input
                OutlinedTextField(
                    value = uiState.description,
                    enabled = !uiState.isSubmitting,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Keterangan (opsional)") },
                    placeholder = { Text("mis. SPBU KM 42 jalur tengah") },
                    maxLines = 3,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Receipts Thumbnails
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Foto struk (${uiState.receipts.size}/8)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (uiState.receipts.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(uiState.receipts) { index, receipt ->
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.small)
                                ) {
                                    EvidenceImage(
                                        source = receipt.dataUrl,
                                        contentDescription = "Struk ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        showStatus = false,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    FilledTonalIconButton(
                                        onClick = { viewModel.removeReceipt(index) },
                                        enabled = !uiState.isSubmitting,
                                        shapes = IconButtonDefaults.shapes(),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(Icons.Default.Close, "Hapus struk ${index + 1}", Modifier.size(18.dp))
                                    }
                                    // GPS tag
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (receipt.capture?.latitude != null) "GPS" else "tanpa GPS",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (rowCapture != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Metadata baris: diambil ${Formatters.formatTime(rowCapture.capturedAt)} ${EvidenceZones.getShortLabel(uiState.timeZone)}" +
                                            if (rowCapture.latitude != null) " · ${String.format(java.util.Locale.US, "%.5f, %.5f", rowCapture.latitude, rowCapture.longitude)}" else " · tanpa GPS",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!rowCapture.locationLabel.isNullOrBlank()) {
                                    Text(
                                        text = rowCapture.locationLabel,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    PrimaryButton(
                        label = "Tambah foto struk",
                        onClick = { viewModel.startAddReceipt(onNavigateToCapture) },
                        enabled = !uiState.isSubmitting && uiState.receipts.size < 8
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::submitExpense,
                enabled = !uiState.isSubmitting && uiState.receipts.isNotEmpty()
            ) {
                Text(if (uiState.isSubmitting) "Menyimpan…" else "Simpan biaya")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeForm, enabled = !uiState.isSubmitting) { Text("Tutup") }
        }
    )
}
