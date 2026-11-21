package com.kaminova.novalogistikmobile.ui.screens.drops

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaminova.novalogistikmobile.data.api.DropShipment
import com.kaminova.novalogistikmobile.ui.components.ExpressiveRefreshBox
import com.kaminova.novalogistikmobile.ui.components.ErrorBanner
import com.kaminova.novalogistikmobile.ui.components.ErrorView
import com.kaminova.novalogistikmobile.ui.components.LoadingView
import com.kaminova.novalogistikmobile.ui.components.MapsLink
import com.kaminova.novalogistikmobile.ui.components.PrimaryButton
import com.kaminova.novalogistikmobile.ui.components.ScreenHeading
import com.kaminova.novalogistikmobile.ui.components.SectionCard
import com.kaminova.novalogistikmobile.ui.components.StatusBadge
import com.kaminova.novalogistikmobile.ui.components.SuccessBanner
import com.kaminova.novalogistikmobile.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DropsScreen(
    viewModel: DropsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingConfirmShipment: DropShipment? by remember { mutableStateOf(null) }

    if (uiState.isLoading && uiState.board == null) {
        LoadingView(label = "Memuat papan multi-drop…")
        return
    }

    if (uiState.error != null && uiState.board == null) {
        ErrorView(message = uiState.error!!, onRetry = { viewModel.loadBoard() })
        return
    }

    val board = uiState.board ?: return
    val transportCode = uiState.transport?.transportCode ?: "#${viewModel.transportId}"
    val checkpointsById = remember(board.checkpoints) {
        board.checkpoints.associateBy { it.id }
    }

    if (pendingConfirmShipment != null) {
        val s = pendingConfirmShipment!!
        val dropName = checkpointsById[s.effectiveDropCheckpointId]?.name ?: s.destination ?: "tujuan resi"
        AlertDialog(
            onDismissRequest = { pendingConfirmShipment = null },
            title = { Text("Turunkan resi ini?", fontWeight = FontWeight.Bold) },
            text = { Text("Konfirmasi bahwa ${s.masterCode} sudah diturunkan di $dropName.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingConfirmShipment = null
                        viewModel.dropShipment(s)
                    }
                ) {
                    Text("Turunkan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmShipment = null }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Papan Multi-Drop", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
                    title = "Multi-drop",
                    subtitle = "Pantau muatan dan penurunan di setiap tujuan.",
                    icon = Icons.Default.Archive
                )

                StatusBadge(status = board.transportStatus)

                if (uiState.notice != null) {
                    SuccessBanner(message = uiState.notice!!)
                }
                if (uiState.actionError != null) {
                    ErrorBanner(message = uiState.actionError!!)
                }

                uiState.error?.let { ErrorBanner(message = "$it Tarik untuk memuat ulang papan.") }

                // Drop Progress Card
                SectionCard(title = "Progres Penurunan") {
                    val progressRatio = if (board.progress.total > 0) {
                        board.progress.dropped.toFloat() / board.progress.total.toFloat()
                    } else 0f

                    Text(
                        text = "${Formatters.formatNumber(board.progress.dropped)} / ${Formatters.formatNumber(board.progress.total)} resi diturunkan" +
                                if (board.vehicleEmpty) " · kendaraan sudah kosong ✓" else "",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    LinearWavyProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Text(
                        text = "Sisa muatan: ${Formatters.formatNumber(board.loadSummary.remainingLoad.totalKoli)} koli · " +
                                "${Formatters.formatNumber(board.loadSummary.remainingLoad.totalActualWeightKg)} KG · " +
                                "${Formatters.formatNumber(board.loadSummary.remainingLoad.totalVolumeM3, 2)} m³",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                // Checkpoints status
                if (board.checkpoints.isNotEmpty()) {
                    SectionCard(title = "Checkpoint Rute") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            board.checkpoints.forEach { cp ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
                                        .border(
                                            width = 1.dp,
                                            color = if (cp.checkedIn) Color(0xFF146C2E) else MaterialTheme.colorScheme.outlineVariant,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${cp.sequence} ${cp.name}${if (cp.isFinal) " (akhir)" else ""}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (cp.checkedIn) "✓ TER-CHECK-IN" else "BELUM",
                                            color = if (cp.checkedIn) Color(0xFF146C2E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Groups of shipments by checkpoint
                board.groups.forEach { group ->
                    val groupShipments = board.shipments.filter { group.shipmentIds.contains(it.shipmentId) }
                    if (groupShipments.isNotEmpty()) {
                        val groupCp = checkpointsById[group.checkpointId]
                        val groupTitle = if (groupCp != null) "Drop: #${groupCp.sequence} ${groupCp.name}" else "Drop: tujuan akhir"

                        SectionCard(
                            title = groupTitle,
                            action = {
                                val tCp = uiState.transport?.checkpoints?.find { it.id == groupCp?.id }
                                if (tCp != null) {
                                    MapsLink(
                                        latitude = tCp.latitude,
                                        longitude = tCp.longitude,
                                        label = tCp.name,
                                        compact = true
                                    )
                                }
                            }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                groupShipments.forEach { shipment ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.medium)
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SelectionContainer {
                                                    Text(
                                                        text = shipment.masterCode,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                StatusBadge(status = shipment.dropStatus)
                                            }
                                            Text(
                                                text = "${shipment.customerName ?: "-"} → ${shipment.destination ?: "-"}" +
                                                        if (!shipment.penerimaName.isNullOrBlank()) " (penerima: ${shipment.penerimaName})" else "",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                            if (!shipment.droppedAt.isNullOrBlank()) {
                                                Text(
                                                    text = "Diturunkan ${Formatters.formatDateTime(shipment.droppedAt)}",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            val canDrop = viewModel.canUnload(shipment)
                                            val isBusy = uiState.busyShipmentId == shipment.shipmentId

                                            if (canDrop) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                PrimaryButton(
                                                    label = "Turunkan resi",
                                                    onClick = { pendingConfirmShipment = shipment },
                                                    loading = isBusy,
                                                    enabled = uiState.busyShipmentId == null && !uiState.isRefreshing && uiState.error == null
                                                )
                                            } else {
                                                Text(
                                                    text = when {
                                                        board.transportStatus == "PLANNED" && shipment.dropStatus == "LOADED" ->
                                                            "Transport harus berangkat sebelum penurunan."
                                                        shipment.dropStatus == "LOADED" && board.trackingMode == "CHECKPOINT" ->
                                                            "Check-in di checkpoint drop terlebih dahulu untuk membuka penurunan."
                                                        else -> "Tidak dapat diturunkan dari status ini."
                                                    },
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Penurunan resi menunggu persetujuan delivery admin. Server memeriksa kelayakan setiap penurunan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
