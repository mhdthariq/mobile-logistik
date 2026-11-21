package com.kaminova.novalogistikmobile

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.imageLoader
import coil.request.SuccessResult
import com.kaminova.novalogistikmobile.ui.components.evidenceImageRequest
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvidenceImageTest {
    @Test
    fun displaysEmbeddedJpegAndPngFromApi() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(24, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        for ((subtype, format) in listOf("jpeg" to Bitmap.CompressFormat.JPEG, "png" to Bitmap.CompressFormat.PNG)) {
            val bytes = ByteArrayOutputStream()
            assertTrue(bitmap.compress(format, 90, bytes))
            val source = "data:image/$subtype;base64,${Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)}"
            val image = context.imageLoader.execute(evidenceImageRequest(context, source))
            assertTrue("$subtype proof did not decode: $image", image is SuccessResult)
        }
        bitmap.recycle()
    }
}
