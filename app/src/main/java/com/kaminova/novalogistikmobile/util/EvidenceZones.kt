package com.kaminova.novalogistikmobile.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class EvidenceZone(
    val id: String,
    val shortLabel: String
)

object EvidenceZones {
    val ZONES = listOf(
        EvidenceZone("Asia/Jakarta", "WIB"),
        EvidenceZone("Asia/Makassar", "WITA"),
        EvidenceZone("Asia/Jayapura", "WIT")
    )

    const val DEFAULT_ZONE_ID = "Asia/Jakarta"

    fun isValidZone(zoneId: String): Boolean = ZONES.any { it.id == zoneId }

    fun getShortLabel(zoneId: String): String {
        return ZONES.find { it.id == zoneId }?.shortLabel ?: zoneId
    }

    fun formatTimeInZone(instant: Instant, zoneId: String): String {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.of(DEFAULT_ZONE_ID))
        val zdt = instant.atZone(zone)
        val formatter = DateTimeFormatter.ofPattern("HH.mm", Locale("id", "ID"))
        return zdt.format(formatter)
    }

    fun formatDateInZone(instant: Instant, zoneId: String): String {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.of(DEFAULT_ZONE_ID))
        val zdt = instant.atZone(zone)
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
        return zdt.format(formatter)
    }

    fun getZoneOffsetLabel(instant: Instant, zoneId: String): String {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.of(DEFAULT_ZONE_ID))
        val zdt = instant.atZone(zone)
        val offset = zdt.offset
        return "UTC${offset.id}"
    }
}
