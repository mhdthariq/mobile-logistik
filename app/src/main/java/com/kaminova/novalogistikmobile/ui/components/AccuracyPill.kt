package com.kaminova.novalogistikmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaminova.novalogistikmobile.ui.theme.AccuracyFair
import com.kaminova.novalogistikmobile.ui.theme.AccuracyGood
import com.kaminova.novalogistikmobile.ui.theme.AccuracyPoor
import com.kaminova.novalogistikmobile.util.Formatters

@Composable
fun AccuracyPill(
    accuracyMeters: Double?,
    approximate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = when {
        approximate && accuracyMeters == null -> AccuracyFair
        accuracyMeters == null || accuracyMeters < 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        accuracyMeters <= 20.0 -> AccuracyGood
        accuracyMeters <= 50.0 -> AccuracyFair
        else -> AccuracyPoor
    }

    val text = when {
        approximate -> if (accuracyMeters == null) "Lokasi perkiraan" else "± ${Formatters.formatDistance(accuracyMeters)} (perkiraan)"
        accuracyMeters == null -> "Menunggu GPS…"
        else -> "± ${Formatters.formatDistance(accuracyMeters)}"
    }

    Box(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surface, shape = CircleShape)
            .border(width = 1.5.dp, color = color, shape = CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DistanceToTarget(
    meters: Long?,
    radiusMeters: Double,
    modifier: Modifier = Modifier
) {
    val inside = meters != null && meters <= radiusMeters
    val bg = if (inside) Color(0xFFC4EED0) else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (inside) Color(0xFF0F5132) else MaterialTheme.colorScheme.onSurfaceVariant

    val text = if (meters == null) {
        "Jarak: menunggu GPS"
    } else {
        "Jarak Anda: ${Formatters.formatDistance(meters)} (radius ${Formatters.formatDistance(radiusMeters)})"
    }

    Box(
        modifier = modifier
            .background(color = bg, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
