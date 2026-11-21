package com.kaminova.novalogistikmobile.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaminova.novalogistikmobile.ui.components.ExpressiveRefreshBox
import com.kaminova.novalogistikmobile.ui.components.ErrorBanner
import com.kaminova.novalogistikmobile.ui.components.ErrorView
import com.kaminova.novalogistikmobile.ui.components.LabeledRow
import com.kaminova.novalogistikmobile.ui.components.LoadingView
import com.kaminova.novalogistikmobile.ui.components.MapsLink
import com.kaminova.novalogistikmobile.ui.components.ModeChip
import com.kaminova.novalogistikmobile.ui.components.PrimaryButton
import com.kaminova.novalogistikmobile.ui.components.ProofPhotos
import com.kaminova.novalogistikmobile.ui.components.SecondaryButton
import com.kaminova.novalogistikmobile.ui.components.SectionCard
import com.kaminova.novalogistikmobile.ui.components.StatTile
import com.kaminova.novalogistikmobile.ui.components.StatusBadge
import com.kaminova.novalogistikmobile.ui.components.SuccessBanner
import com.kaminova.novalogistikmobile.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransportDetailScreen(
    viewModel: TransportDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToExpenses: (Long) -> Unit,
    onNavigateToDrops: (Long) -> Unit,
    onNavigateToCapture: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showProof by remember { mutableStateOf(false) }
    var showDepartDialog by remember { mutableStateOf(false) }
    var showArriveDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading && uiState.transport == null) {
        LoadingView(label = "Memuat detail transport…")
        return
    }

    if (uiState.error != null && uiState.transport == null) {
        ErrorView(
            message = uiState.error!!,
            onRetry = { viewModel.loadDetail() }
        )
        return
    }

    val transport = uiState.transport ?: return
    val checkedInIds = viewModel.getCheckedInIds()
    val nextCheckpoint = viewModel.getNextCheckpoint()
    val isCheckpointMode = transport.trackingMode == "CHECKPOINT"
    if (showProof) {
        val photos by produceState<Result<List<String>>?>(null, transport.id) { value = viewModel.proofPhotos(transport) }
        ProofPhotos("Bukti perjalanan", photos) { showProof = false }
    }

    val canAct = transport.status == "PLANNED" || transport.status == "DEPARTED"
    val departBlocked = isCheckpointMode && !viewModel.isFirstCheckpointCheckedIn()

    // Depart Confirmation Dialog
    if (showDepartDialog) {
        AlertDialog(
            onDismissRequest = { showDepartDialog = false },
            title = { Text("Berangkat sekarang?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Transport ${transport.transportCode} akan ditandai DEPARTED" +
                            if (isCheckpointMode) " (check-in checkpoint pertama sudah tercatat)." else "."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDepartDialog = false
                        viewModel.depart()
                    }
                ) {
                    Text("Berangkat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepartDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Arrive Confirmation Dialog
    if (showArriveDialog) {
        val needsEvidence = transport.trackingMode == "ACTIVITY" && !viewModel.canArriveWithoutPhoto
        AlertDialog(
            onDismissRequest = { showArriveDialog = false },
            title = { Text("Tiba di tujuan?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (needsEvidence) {
                        "Anda akan diminta memfoto bukti tiba (mode ACTIVITY)."
                    } else {
                        "Transport ${transport.transportCode} akan ditandai TIBA."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArriveDialog = false
                        if (needsEvidence) {
                            viewModel.startArriveWithPhoto(onNavigateToCapture)
                        } else {
                            viewModel.arrive()
                        }
                    }
                ) {
                    Text("Lanjut", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArriveDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transport", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
                // Headline
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SelectionContainer {
                            Text(
                                text = transport.transportCode,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )
                        }
                        StatusBadge(status = transport.status)
                    }
                    Text(
                        text = "${transport.origin ?: "?"} → ${transport.destination ?: "?"}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "${transport.routeName ?: "Tanpa rute"} · dibuat ${Formatters.formatDate(transport.createdAt)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    ModeChip(
                        trackingMode = transport.trackingMode,
                        transportMode = transport.transportMode
                    )
                }

                // Notices & Banners
                if (uiState.actionNotice != null) {
                    SuccessBanner(message = uiState.actionNotice!!)
                }
                if (uiState.actionError != null) {
                    ErrorBanner(message = uiState.actionError!!)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Biaya Transport", onClick = { onNavigateToExpenses(transport.id) }, modifier = Modifier.weight(1f))
                    SecondaryButton("Bukti Perjalanan", onClick = { showProof = true }, modifier = Modifier.weight(1f))
                }

                // Action Step Card
                if (canAct) {
                    SectionCard(title = "Langkah berikutnya") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Depart button
                            if (transport.status == "PLANNED" && viewModel.canDepart) {
                                PrimaryButton(
                                    label = "Berangkat Sekarang",
                                    onClick = { showDepartDialog = true },
                                    loading = uiState.busyAction == "depart",
                                    enabled = !departBlocked && uiState.busyAction == null
                                )
                            }
                            if (transport.status == "PLANNED" && departBlocked) {
                                Text(
                                    text = "Check-in pertama diperlukan sebelum berangkat.",
                                    color = Color(0xFFB26A00),
                                    fontSize = 13.sp
                                )
                            }

                            // Checkin button (CHECKPOINT mode)
                            if (isCheckpointMode && nextCheckpoint != null && viewModel.canCheckin) {
                                PrimaryButton(
                                    label = "Check-in Selfie · ${nextCheckpoint.name}",
                                    onClick = { viewModel.startCheckin(onNavigateToCapture) },
                                    loading = uiState.busyAction == "checkin",
                                    enabled = uiState.busyAction == null
                                )
                            }
                            if (isCheckpointMode && nextCheckpoint == null) {
                                Text(
                                    text = "Semua checkpoint sudah ter-check-in.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }

                            // Activity report button (ACTIVITY mode)
                            if (!isCheckpointMode && viewModel.canCheckin) {
                                PrimaryButton(
                                    label = "Lapor Aktivitas",
                                    onClick = { viewModel.startReportActivity(onNavigateToCapture) },
                                    loading = uiState.busyAction == "activity",
                                    enabled = uiState.busyAction == null
                                )
                            }

                            // Arrive button
                            if (viewModel.canArrive) {
                                SecondaryButton(
                                    label = "Tandai Tiba di Tujuan",
                                    onClick = { showArriveDialog = true },
                                    enabled = uiState.busyAction == null
                                )
                            }

                            if (transport.transportMode == "MULTI_DROP") {
                                SecondaryButton(label = "Papan Multi-Drop", onClick = { onNavigateToDrops(transport.id) })
                            }
                        }

                    }
                }

                // Schedule section
                SectionCard(title = "Jadwal") {
                    LabeledRow(label = "Rencana Berangkat", value = Formatters.formatDateTime(transport.plannedDepartureAt))
                    LabeledRow(label = "Rencana Tiba", value = Formatters.formatDateTime(transport.plannedArrivalAt))
                    LabeledRow(label = "Berangkat Aktual", value = Formatters.formatDateTime(transport.departedAt))
                    LabeledRow(label = "Tiba Aktual", value = Formatters.formatDateTime(transport.arrivedAt))
                    LabeledRow(label = "Selesai", value = Formatters.formatDateTime(transport.completedAt))
                }

                // Vehicle & Crew section
                SectionCard(title = "Kendaraan & Kru") {
                    LabeledRow(label = "Nomor Polisi", value = transport.vehicle.vehicleNumber, mono = true)
                    LabeledRow(label = "Kendaraan", value = transport.vehicle.name)
                    LabeledRow(
                        label = "Kapasitas",
                        value = "${Formatters.formatNumber(transport.vehicle.maxWeightKg)} KG · ${Formatters.formatNumber(transport.vehicle.maxVolumeM3, 2)} m³" +
                                (if (transport.vehicle.maxKoli != null) " · ${Formatters.formatNumber(transport.vehicle.maxKoli)} koli" else "")
                    )
                    LabeledRow(label = "Driver", value = transport.driver?.name)
                    LabeledRow(label = "Kenek", value = transport.kenek?.name)
                    if (transport.warehouseStaff != null) {
                        LabeledRow(label = "Staff Gudang", value = transport.warehouseStaff.name)
                    }
                }

                // Cargo section
                SectionCard(title = "Muatan") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(label = "Shipment", value = Formatters.formatNumber(transport.shipmentCount), modifier = Modifier.weight(1f))
                        StatTile(label = "Berat", value = "${Formatters.formatNumber(transport.totalWeightKg)} KG", modifier = Modifier.weight(1f))
                        StatTile(label = "Koli", value = Formatters.formatNumber(transport.totalKoli), modifier = Modifier.weight(1f))
                    }
                    LabeledRow(label = "Volume", value = "${Formatters.formatNumber(transport.totalVolumeM3, 2)} m³")
                    if (transport.capacity != null) {
                        LabeledRow(label = "Status Kapasitas", value = transport.capacity["status"]?.toString())
                    }
                }

                // Checkpoints (CHECKPOINT mode)
                if (isCheckpointMode && transport.checkpoints.isNotEmpty()) {
                    SectionCard(title = "Checkpoints (${transport.checkpoints.size})") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            transport.checkpoints.forEach { checkpoint ->
                                val done = checkedInIds.contains(checkpoint.id)
                                val isNext = nextCheckpoint?.id == checkpoint.id
                                val record = transport.checkpointRecords.find { it.checkpointId == checkpoint.id }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (done) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .border(
                                            width = if (isNext) 2.dp else 1.dp,
                                            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${checkpoint.sequence} ${checkpoint.name}",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (done) "✓ TER-CHECK-IN" else "MENUNGGU",
                                                color = if (done) Color(0xFF146C2E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        SelectionContainer {
                                            Text(
                                                text = "Radius ${Formatters.formatDistance(checkpoint.radiusMeters)} · ${String.format(java.util.Locale.US, "%.5f, %.5f", checkpoint.latitude, checkpoint.longitude)}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        if (record != null) {
                                            Text(
                                                text = "${Formatters.formatDateTime(record.recordedAt)} · ${record.recordedBy?.name ?: "-"} · jarak ${Formatters.formatDistance(record.distanceMeters)}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        }

                                        MapsLink(
                                            latitude = checkpoint.latitude,
                                            longitude = checkpoint.longitude,
                                            label = checkpoint.name,
                                            compact = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Shipments list
                SectionCard(title = "Shipment (${transport.shipments.size})") {
                    if (transport.shipments.isEmpty()) {
                        Text(
                            text = "Belum ada shipment pada transport ini.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            transport.shipments.forEach { shipment ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
                                        .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.small)
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SelectionContainer {
                                                Text(
                                                    text = shipment.masterCode,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            StatusBadge(status = shipment.status)
                                        }
                                        Text(
                                            text = "${shipment.customerName ?: "-"} → ${shipment.destination ?: "-"}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${Formatters.formatNumber(shipment.packages)} koli · ${Formatters.formatNumber(shipment.weightKg, 1)} KG · ${Formatters.formatNumber(shipment.volumeM3, 2)} m³" +
                                                    (if (!shipment.penerimaName.isNullOrBlank()) " · penerima: ${shipment.penerimaName}" else ""),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Check-in history timeline
                if (transport.checkpointRecords.isNotEmpty()) {
                    SectionCard(title = "Riwayat Check-in") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            transport.checkpointRecords.forEach { record ->
                                LabeledRow(
                                    label = record.checkpointName,
                                    value = "${Formatters.formatDateTime(record.recordedAt)} · ${record.recordedBy?.name ?: "-"}"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
