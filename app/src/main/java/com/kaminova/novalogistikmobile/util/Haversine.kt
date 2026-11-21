package com.kaminova.novalogistikmobile.util

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Great-circle distance in meters - mirrors backend `haversineMeters`
 * so the client's "distance to checkpoint" indicator matches what the server recomputes on check-in.
 */
object Haversine {
    private const val EARTH_RADIUS_METERS = 6371000.0

    fun calculateMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Long {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return Math.round(EARTH_RADIUS_METERS * c)
    }
}
