package com.kaminova.novalogistikmobile.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaminova.novalogistikmobile.ui.components.ExpressiveRefreshBox
import com.kaminova.novalogistikmobile.ui.components.EmptyView
import com.kaminova.novalogistikmobile.ui.components.ErrorView
import com.kaminova.novalogistikmobile.ui.components.LoadingView
import com.kaminova.novalogistikmobile.ui.components.ScreenHeading
import com.kaminova.novalogistikmobile.ui.components.TransportCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredList by viewModel.filteredList.collectAsState()

    if (uiState.isLoading && uiState.rawList.isEmpty()) {
        LoadingView(label = "Memuat riwayat transport…")
        return
    }

    if (uiState.error != null && uiState.rawList.isEmpty()) {
        ErrorView(
            message = uiState.error!!,
            onRetry = { viewModel.loadHistory() }
        )
        return
    }

    ExpressiveRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScreenHeading(
                    eyebrow = "PERJALANAN ANDA",
                    title = "Riwayat",
                    subtitle = "Catatan rute dan perjalanan yang telah dilalui.",
                    icon = Icons.Default.History
                )

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    placeholder = { Text("Cari kode, rute, atau nomor polisi…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Text(
                    text = "${filteredList.size} perjalanan" + (if (uiState.searchQuery.isNotBlank()) " ditemukan" else ""),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.padding(20.dp)) {
                    EmptyView(
                        title = if (uiState.searchQuery.isNotBlank()) "Tidak ada hasil." else "Belum ada riwayat transport.",
                        hint = if (uiState.searchQuery.isNotBlank()) "Coba ubah atau hapus kata pencarian." else "Transport selesai akan muncul di sini."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { transport ->
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
        }
    }
}
