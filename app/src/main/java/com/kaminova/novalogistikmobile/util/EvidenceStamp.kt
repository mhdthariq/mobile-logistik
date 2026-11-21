package com.kaminova.novalogistikmobile.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.kaminova.novalogistikmobile.R
import com.kaminova.novalogistikmobile.data.api.VehicleDetail
import com.kaminova.novalogistikmobile.data.api.UserSummary
import java.time.Instant
import java.util.Locale

internal data class EvidenceStampData(
    val company: String,
    val title: String,
    val capturedAt: Instant,
    val timeZone: String,
    val fix: LocationFix?,
    val address: String?,
    val context: List<String>,
    val vehicle: VehicleDetail? = null,
    val driver: UserSummary? = null
)

internal class EvidenceStamp(context: Context) {
    private val regular = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.DEFAULT
    private val bold = Typeface.create(regular, Typeface.BOLD)

    fun draw(canvas: Canvas, width: Float, height: Float, data: EvidenceStampData) {
        if (width <= 0 || height <= 0) return
        val scale = minOf(width, height) / 480f
        val w = width / scale
        val h = height / scale
        val inset = 16f
        val cardWidth = w - inset * 2
        val padding = 14f
        val textWidth = cardWidth - padding * 2
        val white = 0xFFFFFFFF.toInt()

        fun line(value: String, size: Float, strong: Boolean = false, color: Int = white,
                 lines: Int = 1, available: Float = textWidth): StaticLayout {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                typeface = if (strong) bold else regular
                this.color = color
            }
            val text = value.replace(Regex("\\s+"), " ").trim()
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, available.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setMaxLines(lines)
                .build()
        }

        fun panel(x: Float, y: Float, width: Float, height: Float, color: Int) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            canvas.drawRoundRect(x, y, x + width, y + height, 18f, 18f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = 0x4DFFFFFF
            canvas.drawRoundRect(x + .5f, y + .5f, x + width - .5f, y + height - .5f, 18f, 18f, paint)
        }

        fun place(layout: StaticLayout, x: Float, y: Float) {
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        }

        val ink = 0xFF102E28.toInt()
        val green = 0xE6075544.toInt()
        val gold = 0xFFFFCE35.toInt()
        val light = 0xEFFFFFFF.toInt()
        val gpsWidth = 106f
        val brandWidth = textWidth - gpsWidth - 12
        val company = line(data.company, 14f, true, lines = 2, available = brandWidth)
        val title = line(data.title, 11f, color = 0xFFBCE4D6.toInt(), lines = 2, available = brandWidth)
        val headerHeight = company.height + title.height + 26f
        val location = data.fix
        val accuracy = location?.accuracy?.let { "GPS ±${it.toInt()} m" } ?: if (location == null) "Menunggu GPS" else "GPS tersedia"
        val gps = line(accuracy, 11f, true, ink, available = gpsWidth - 16)
        val coordinates = location?.let { String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude) }

        val timeWidth = textWidth * .42f
        val tripWidth = textWidth - timeWidth - 12
        val time = line(EvidenceZones.formatTimeInZone(data.capturedAt, data.timeZone), 22f, true, ink, available = timeWidth)
        val date = line(EvidenceZones.formatDateInZone(data.capturedAt, data.timeZone), 10f, color = ink, lines = 2, available = timeWidth)
        val code = data.context.firstOrNull()?.takeIf(String::isNotBlank)?.let {
            line(it, 13f, true, ink, available = tripWidth)
        }
        val trip = data.context.drop(1).filter(String::isNotBlank).joinToString(" · ").takeIf(String::isNotEmpty)?.let {
            line(it, 10f, color = ink, lines = 2, available = tripWidth)
        }
        val summaryHeight = maxOf(time.height + date.height + 2, (code?.height ?: 0) + (trip?.height ?: 0) + 2)

        val vehicle = data.vehicle
        val locWidth = if (vehicle == null) textWidth else textWidth * .54f
        val vehicleWidth = textWidth - locWidth - 10
        val addressLabel = line("Alamat lokasi", 10f, color = ink, available = locWidth)
        val address = line(data.address?.takeIf(String::isNotBlank) ?: if (location == null) "GPS belum tersedia" else "Alamat tidak tersedia",
            12f, true, ink, 3, locWidth)
        val coord = coordinates?.let { line(it, 10f, color = ink, available = locWidth) }
        val mock = if (location?.isMock == true) line("Lokasi tiruan", 11f, true, 0xFF9B301F.toInt(), available = locWidth) else null
        val vehicleLabel = vehicle?.let { line("Kendaraan", 10f, color = ink, available = vehicleWidth) }
        val plate = vehicle?.let { line(it.vehicleNumber, 14f, true, ink, available = vehicleWidth - 12) }
        val vehicleName = vehicle?.let {
            val details = listOfNotNull(it.name?.takeIf(String::isNotBlank), it.wheelCount?.takeIf { n -> n > 0 }?.let { n -> "$n roda" })
            line(details.joinToString(" · "), 10f, color = ink, lines = 2, available = vehicleWidth)
        }
        val locationHeight = addressLabel.height + address.height + (coord?.height ?: 0) + (mock?.height ?: 0) + 8
        val vehicleHeight = (vehicleLabel?.height ?: 0) + (plate?.height ?: 0) + (vehicleName?.height ?: 0) + 15
        val detailHeight = maxOf(locationHeight, vehicleHeight)
        val driver = data.driver
        val phone = driver?.phone?.takeIf(String::isNotBlank)
        val driverName = driver?.let { line(it.name, 12f, true, ink, available = if (phone == null) textWidth else textWidth / 2 - 8) }
        val phoneText = phone?.let { line(it, 12f, true, ink, available = textWidth / 2 - 8) }
        val driverLabel = line("Sopir", 10f, color = ink)
        val phoneLabel = line("No. HP sopir", 10f, color = ink)
        val crewHeight = if (driverName == null) 0 else driverLabel.height + maxOf(driverName.height, phoneText?.height ?: 0) + 11
        val footerHeight = summaryHeight + detailHeight + crewHeight + 37
        val footerTop = h - inset - footerHeight

        fun rule(x: Float, y: Float, endX: Float, endY: Float, color: Int) {
            canvas.drawLine(x, y, endX, endY, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        }
        canvas.save()
        canvas.scale(scale, scale)
        panel(inset, inset, cardWidth, headerHeight, green)
        val left = inset + padding
        place(company, left, inset + 12)
        place(title, left, inset + 14 + company.height)
        val gpsX = w - inset - padding - gpsWidth
        val gpsY = inset + (headerHeight - gps.height - 12) / 2
        canvas.drawRoundRect(gpsX, gpsY, gpsX + gpsWidth, gpsY + gps.height + 12, 18f, 18f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (location == null) 0xFFFFE3A7.toInt() else 0xFFB1F1D6.toInt() })
        place(gps, gpsX + (gpsWidth - gps.getLineWidth(0)) / 2, gpsY + 6)

        panel(inset, footerTop, cardWidth, footerHeight.toFloat(), light)
        val summaryTop = footerTop + 11
        place(time, left, summaryTop)
        place(date, left, summaryTop + time.height + 2)
        val tripX = left + timeWidth + 12
        code?.let { place(it, tripX, summaryTop + 2) }
        trip?.let { place(it, tripX, summaryTop + (code?.height ?: 0) + 4) }
        val detailTop = summaryTop + summaryHeight + 14
        rule(left, detailTop - 7, left + textWidth, detailTop - 7, 0x35102E28)
        place(addressLabel, left, detailTop)
        place(address, left, detailTop + addressLabel.height + 2)
        val coordsY = detailTop + addressLabel.height + address.height + 4
        coord?.let { place(it, left, coordsY) }
        mock?.let { place(it, left, coordsY + (coord?.height ?: 0) + 2) }
        if (vehicleLabel != null && plate != null) {
            val x = left + locWidth + 10
            rule(x - 7, detailTop, x - 7, detailTop + detailHeight - 4, 0x35102E28)
            place(vehicleLabel, x, detailTop)
            val y = detailTop + vehicleLabel.height + 3
            val plateWidth = minOf(vehicleWidth, plate.getLineWidth(0) + 12)
            val plateColor = when (vehicle.plateColor?.uppercase(Locale.ROOT)) {
                "YELLOW" -> gold
                "WHITE" -> white
                else -> 0xFFE1EAE6.toInt()
            }
            canvas.drawRoundRect(x, y, x + plateWidth, y + plate.height + 6, 4f, 4f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = plateColor })
            place(plate, x + 6, y + 3)
            vehicleName?.let { place(it, x, y + plate.height + 8) }
        }
        if (driverName != null) {
            val y = detailTop + detailHeight + 6
            rule(left, y - 5, left + textWidth, y - 5, 0x35102E28)
            place(driverLabel, left, y)
            place(driverName, left, y + driverLabel.height + 2)
            if (phoneText != null) {
                val x = left + textWidth / 2
                place(phoneLabel, x, y)
                place(phoneText, x, y + phoneLabel.height + 2)
            }
        }
        canvas.restore()
    }
}
