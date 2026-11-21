package com.kaminova.novalogistikmobile

import com.google.gson.Gson
import com.kaminova.novalogistikmobile.data.api.TransportDetail
import com.kaminova.novalogistikmobile.data.api.TransportStatusUpdate
import com.kaminova.novalogistikmobile.data.api.UserSummary
import com.kaminova.novalogistikmobile.data.api.VehicleDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class TransportStatusUpdateTest {
    @Test
    fun departureResponsePreservesEmployeeContactAndVehicle() {
        val loaded = TransportDetail(
            id = 1,
            transportCode = "TRP-1",
            status = "PLANNED",
            trackingMode = "ACTIVITY",
            transportMode = "DIRECT",
            vehicle = VehicleDetail(1, "BK 1234 XY"),
            driver = UserSummary(2, "Sopir Tes", "081234567890")
        )
        val update = Gson().fromJson(
            """{"status":"DEPARTED","departedAt":"2026-11-21T01:00:00Z"}""",
            TransportStatusUpdate::class.java
        )
        val departed = update.applyTo(loaded)

        assertEquals("DEPARTED", departed.status)
        assertEquals("2026-11-21T01:00:00Z", departed.departedAt)
        assertEquals(loaded.driver, departed.driver)
        assertEquals(loaded.vehicle, departed.vehicle)
    }
}
