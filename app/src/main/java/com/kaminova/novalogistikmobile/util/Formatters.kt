package com.kaminova.novalogistikmobile.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object Formatters {
    private val indonesianLocale = Locale("id", "ID")

    fun formatNumber(value: Number?, digits: Int = 0): String {
        if (value == null) return "-"
        val nf = NumberFormat.getNumberInstance(indonesianLocale).apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        return nf.format(value)
    }

    fun formatRupiah(value: Number?): String {
        if (value == null) return "-"
        return "Rp ${formatNumber(value, 0)}"
    }

    fun formatDateTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            val instant = Instant.parse(iso)
            val zdt = instant.atZone(ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", indonesianLocale).format(zdt)
        } catch (_: Exception) {
            iso
        }
    }

    fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            val instant = Instant.parse(iso)
            val zdt = instant.atZone(ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("d MMM yyyy", indonesianLocale).format(zdt)
        } catch (_: Exception) {
            iso
        }
    }

    fun formatTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            val instant = Instant.parse(iso)
            val zdt = instant.atZone(ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("HH.mm", indonesianLocale).format(zdt)
        } catch (_: Exception) {
            iso
        }
    }

    fun formatDistance(meters: Number?): String {
        if (meters == null) return "-"
        val m = meters.toDouble()
        if (m < 0) return "-"
        return if (m < 1000) {
            "${m.roundToInt()} m"
        } else {
            val km = m / 1000.0
            "${formatNumber(km, 1)} km"
        }
    }

    fun todayIsoDate(): String {
        return LocalDate.now().toString()
    }

    fun greeting(): String {
        val hour = LocalTime.now().hour
        return when {
            hour < 11 -> "Selamat pagi"
            hour < 15 -> "Selamat siang"
            hour < 18 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }
}
