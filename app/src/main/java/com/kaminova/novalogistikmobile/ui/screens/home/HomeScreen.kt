package com.kaminova.novalogistikmobile.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kaminova.novalogistikmobile.data.api.DriverTransportSummary
import com.kaminova.novalogistikmobile.ui.components.ExpressiveRefreshBox
import com.kaminova.novalogistikmobile.ui.components.AccuracyPill
import com.kaminova.novalogistikmobile.ui.components.DistanceToTarget
import com.kaminova.novalogistikmobile.ui.components.EmptyView
import com.kaminova.novalogistikmobile.ui.components.ErrorBanner
import com.kaminova.novalogistikmobile.ui.components.ErrorView
import com.kaminova.novalogistikmobile.ui.components.LabeledRow
import com.kaminova.novalogistikmobile.ui.components.LoadingView
import com.kaminova.novalogistikmobile.ui.components.MapsLink
import com.kaminova.novalogistikmobile.ui.components.PrimaryButton
import com.kaminova.novalogistikmobile.ui.components.SectionCard
import com.kaminova.novalogistikmobile.ui.components.StatTile
import com.kaminova.novalogistikmobile.ui.components.StatusBadge
import com.kaminova.novalogistikmobile.ui.components.TransportCard
import com.kaminova.novalogistikmobile.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startLocationWatch()
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startLocationWatch()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocationWatch()
        }
    }

    if (uiState.isLoading && uiState.data == null) {
        LoadingView(label = "Memuat transport yang ditugaskan…")
        return
    }

    if (uiState.error != null && uiState.data == null) {
        ErrorView(
            message = uiState.error!!,
            onRetry = { viewModel.loadDashboard() }
        )
        return
    }

    val dashboard = uiState.data ?: return
    val current = dashboard.current
    val upcoming = dashboard.upcoming
    val future = dashboard.future
    val history = dashboard.history
    val firstName = uiState.session?.user?.name?.split(" ")?.firstOrNull() ?: "Anda"

    ExpressiveRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Nova Logistik",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${Formatters.greeting()},",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = firstName,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.session?.companyName.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                FilledTonalIconButton(
                    onClick = onNavigateToProfile,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(56.dp).semantics { contentDescription = "Buka profil" }
                ) {
                    Text(
                        text = firstName.firstOrNull()?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Note if present
            if (!dashboard.note.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xFFFFE4BE), shape = MaterialTheme.shapes.small)
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = dashboard.note,
                            color = Color(0xFF835C00),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (uiState.error != null) {
                ErrorBanner(message = uiState.error!!)
            }

            // Current Active Transport
            if (current != null) {
                SectionCard(
                    title = "Perjalanan aktif",
                    action = { StatusBadge(status = current.status) }
                ) {
                    SelectionContainer {
                        Text(
                            text = current.transportCode,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Route
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(26.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column {
                                Text(text = "Asal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text(
                                    text = current.origin ?: "Belum ditentukan",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(text = "Tujuan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text(
                                    text = current.destination ?: "Belum ditentukan",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(label = "Shipment", value = Formatters.formatNumber(current.shipmentCount), modifier = Modifier.weight(1f))
                        StatTile(label = "Berat", value = "${Formatters.formatNumber(current.totalWeightKg)} kg", modifier = Modifier.weight(1f))
                        StatTile(label = "Volume", value = "${Formatters.formatNumber(current.totalVolumeM3, 2)} m³", modifier = Modifier.weight(1f))
                    }

                    // Vehicle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = current.vehicleNumber,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Next Checkpoint Panel
                    NextCheckpointCard(
                        transport = current,
                        liveAccuracy = uiState.liveFix?.accuracy,
                        distanceMeters = uiState.distanceToNextMeters
                    )

                    // Open Transport Button
                    PrimaryButton(
                        label = if (current.trackingMode == "ACTIVITY") "Lapor aktivitas" else "Buka transport",
                        onClick = { onNavigateToDetail(current.id) }
                    )

                    TextButton(
                        onClick = viewModel::toggleTripDetails,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.showTripDetails) "Tutup rincian perjalanan" else "Rincian perjalanan")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (uiState.showTripDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = uiState.showTripDetails) {
                        Column {
                            LabeledRow(label = "Kendaraan", value = current.vehicleName)
                            LabeledRow(label = "Driver", value = current.driverName)
                            LabeledRow(label = "Kenek", value = current.kenekName)
                            LabeledRow(label = "Berangkat", value = Formatters.formatDateTime(current.departedAt))
                        }
                    }
                }
            } else {
                EmptyView(
                    title = "Tidak ada transport yang sedang berjalan.",
                    hint = if (upcoming.isNotEmpty()) {
                        "Transport berikutnya: ${upcoming.first().transportCode} (${Formatters.formatDateTime(upcoming.first().plannedDepartureAt)})."
                    } else {
                        "Transport baru muncul di sini setelah admin menugaskannya kepada Anda."
                    }
                )
            }

            // Upcoming Section
            SectionCard(title = "Transport berikutnya") {
                if (upcoming.isEmpty()) {
                    ScheduleEmptyView(message = "Belum ada jadwal berikutnya")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        upcoming.forEach { transport ->
                            TransportCard(
                                transportCode = transport.transportCode,
                                status = transport.status,
                                origin = transport.origin,
                                destination = transport.destination,
                                vehicleNumber = transport.vehicleNumber,
                                shipmentCount = transport.shipmentCount,
                                totalWeightKg = transport.totalWeightKg,
                                plannedDepartureAt = transport.plannedDepartureAt,
                                onClick = { onNavigateToDetail(transport.id) }
                            )
                        }
                    }
                }
            }

            // Future Section
            SectionCard(title = "Transport mendatang") {
                if (future.isEmpty()) {
                    ScheduleEmptyView(message = "Belum ada jadwal mendatang")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        future.forEach { transport ->
                            TransportCard(
                                transportCode = transport.transportCode,
                                status = transport.status,
                                origin = transport.origin,
                                destination = transport.destination,
                                vehicleNumber = transport.vehicleNumber,
                                shipmentCount = transport.shipmentCount,
                                totalWeightKg = transport.totalWeightKg,
                                plannedDepartureAt = transport.plannedDepartureAt,
                                onClick = { onNavigateToDetail(transport.id) }
                            )
                        }
                    }
                }
            }

            // Summary Section
            SectionCard(title = "Ringkasan perjalanan") {
                Text(
                    text = "Hasil perjalanan Anda",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        label = "Selesai hari ini",
                        value = Formatters.formatNumber(dashboard.completedCount),
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Total perjalanan",
                        value = Formatters.formatNumber(dashboard.totalTransportsAllTime),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (history.isEmpty()) {
                    EmptyView(
                        title = "Belum ada transport selesai hari ini.",
                        hint = "Riwayat lengkap ada di tab Riwayat."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        history.forEach { transport ->
                            TransportCard(
                                transportCode = transport.transportCode,
                                status = transport.status,
                                origin = transport.origin,
                                destination = transport.destination,
                                vehicleNumber = transport.vehicleNumber,
                                shipmentCount = transport.shipmentCount,
                                totalWeightKg = transport.totalWeightKg,
                                plannedDepartureAt = transport.plannedDepartureAt,
                                actionLabel = "Riwayat",
                                onClick = { onNavigateToDetail(transport.id) }
                            )
                        }
                    }
                }

                TextButton(onClick = onNavigateToHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("Lihat semua riwayat")
                }
            }
        }
    }
}

@Composable
fun NextCheckpointCard(
    transport: DriverTransportSummary,
    liveAccuracy: Double?,
    distanceMeters: Long?
) {
    val next = transport.nextCheckpoint

    if (transport.trackingMode != "CHECKPOINT") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.small)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Laporan aktivitas",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kirim kabar perjalanan secara berkala.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }
        return
    }

    if (next == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color(0xFFC4EED0), shape = MaterialTheme.shapes.small)
                .padding(14.dp)
        ) {
            Text(
                text = "Semua checkpoint sudah ter-check-in (${transport.checkpointsCheckedIn}/${transport.checkpointsTotal}).",
                color = Color(0xFF0F5132),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CHECKPOINT BERIKUTNYA",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "#${next.sequence} ${next.name}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            SelectionContainer {
                Text(
                    text = "Radius validasi ${Formatters.formatDistance(next.radiusMeters)} · ${String.format(java.util.Locale.US, "%.5f, %.5f", next.latitude, next.longitude)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccuracyPill(accuracyMeters = liveAccuracy)
                DistanceToTarget(meters = distanceMeters, radiusMeters = next.radiusMeters)
            }

            MapsLink(
                latitude = next.latitude,
                longitude = next.longitude,
                label = next.name,
                compact = true
            )

            Text(
                text = "Progress checkpoint: ${transport.checkpointsCheckedIn}/${transport.checkpointsTotal} ter-check-in",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ScheduleEmptyView(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Penugasan dari admin akan muncul di sini.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}
