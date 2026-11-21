package com.kaminova.novalogistikmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaminova.novalogistikmobile.ui.theme.*

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status.uppercase()) {
        "PLANNED" -> Triple(StatusPlannedBg, StatusPlannedFg, "TERENCANA")
        "DEPARTED" -> Triple(StatusDepartedBg, StatusDepartedFg, "BERANGKAT")
        "ARRIVED" -> Triple(StatusArrivedBg, StatusArrivedFg, "TIBA")
        "COMPLETED" -> Triple(StatusArrivedBg, StatusArrivedFg, "SELESAI")
        "CANCELLED" -> Triple(StatusCancelledBg, StatusCancelledFg, "DIBATALKAN")
        "PENDING" -> Triple(StatusPendingBg, StatusPendingFg, "PENDING")
        "APPROVED" -> Triple(StatusArrivedBg, StatusArrivedFg, "DISETUJUI")
        "REJECTED" -> Triple(StatusCancelledBg, StatusCancelledFg, "DITOLAK")
        "LOADED" -> Triple(StatusDepartedBg, StatusDepartedFg, "DIMUAT")
        "AT_DROP_POINT" -> Triple(StatusPendingBg, StatusPendingFg, "DI TITIK DROP")
        "DROPPED" -> Triple(StatusArrivedBg, StatusArrivedFg, "DITURUNKAN")
        "DELIVERY_PENDING" -> Triple(StatusPendingBg, StatusPendingFg, "MENUNGGU DELIVERY")
        "DELIVERY_APPROVED" -> Triple(StatusArrivedBg, StatusArrivedFg, "DELIVERY OK")
        else -> Triple(Color(0xFFE7E0EC), Color(0xFF49454F), status)
    }

    Box(
        modifier = modifier
            .background(color = bg, shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ModeChip(
    trackingMode: String?,
    transportMode: String?,
    modifier: Modifier = Modifier
) {
    val parts = mutableListOf<String>()
    if (trackingMode == "CHECKPOINT") parts.add("CHECKPOINT")
    if (trackingMode == "ACTIVITY") parts.add("ACTIVITY")
    if (transportMode == "MULTI_DROP") parts.add("MULTI-DROP")

    if (parts.isEmpty()) return

    Box(
        modifier = modifier
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = parts.joinToString(" · "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
