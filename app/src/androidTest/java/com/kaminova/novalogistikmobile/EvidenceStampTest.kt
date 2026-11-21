package com.kaminova.novalogistikmobile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kaminova.novalogistikmobile.util.EvidenceStamp
import com.kaminova.novalogistikmobile.util.EvidenceStampData
import com.kaminova.novalogistikmobile.util.LocationFix
import java.time.Instant
import com.kaminova.novalogistikmobile.data.api.UserSummary
import com.kaminova.novalogistikmobile.data.api.VehicleDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvidenceStampTest {
    @Test
    fun stampLeavesCenterClearAndShowsPhotoThroughPanels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val renderer = EvidenceStamp(context)
        val data = EvidenceStampData(
            "CV Rajawali Jaya Logistik", "Foto Aktivitas",
            Instant.parse("2026-11-20T01:59:00Z"), "Asia/Jakarta",
            LocationFix(3.659818, 98.675799, 32.0, false, 0L),
            "Mabar, Medan Deli, Kota Medan, Sumatera Utara",
            listOf("TRP-2026-000042", "Banda Aceh → Medan", "1 kiriman"),
            VehicleDetail(1, "L 9040 NO", "Mitsubishi Canter", plateColor = "YELLOW", wheelCount = 6),
            UserSummary(1, "Rhamad Kurniadi", "0821-8080-0104")
        )
        for ((width, height) in listOf(480 to 640, 640 to 480)) {
            val photo = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            photo.eraseColor(Color.BLACK)
            renderer.draw(Canvas(photo), width.toFloat(), height.toFloat(), data)
            assertEquals(Color.BLACK, photo.getPixel(width / 2, height / 2))
            assertTrue(Color.red(photo.getPixel(width - 42, height - 35)) in 215..251)
            val gold = Color.rgb(255, 206, 53)
            val yellowPixels = (height / 2 until height).flatMap { y ->
                (0 until width).filter { x -> photo.getPixel(x, y) == gold }.map { x -> x to y }
            }
            photo.eraseColor(Color.BLACK)
            renderer.draw(Canvas(photo), width.toFloat(), height.toFloat(), data.copy(vehicle = data.vehicle?.copy(plateColor = "WHITE")))
            assertTrue(yellowPixels.any { (x, y) -> photo.getPixel(x, y) == Color.WHITE })
            val contactArea = Bitmap.createBitmap(photo, width / 2, height - 50, width / 2, 28)
            photo.eraseColor(Color.BLACK)
            renderer.draw(Canvas(photo), width.toFloat(), height.toFloat(), data.copy(
                vehicle = data.vehicle?.copy(plateColor = "WHITE"),
                driver = data.driver?.copy(phone = null)
            ))
            val withoutContact = Bitmap.createBitmap(photo, width / 2, height - 50, width / 2, 28)
            assertTrue(!contactArea.sameAs(withoutContact))
            contactArea.recycle()
            withoutContact.recycle()
            photo.recycle()
        }
    }
}
