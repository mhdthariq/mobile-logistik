package com.kaminova.novalogistikmobile.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object ImageCompressor {
    const val MAX_DATA_URL_LENGTH = 2_500_000

    data class CompressedResult(
        val dataUrl: String,
        val width: Int,
        val height: Int,
        val bitmap: Bitmap
    )

    fun compressImageFile(file: File, decorate: (Bitmap) -> Unit): CompressedResult {
        // Read orientation
        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        // Decode bounds
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val origWidth = options.outWidth
        val origHeight = options.outHeight

        // First pass: target 1280px max dimension, quality 80
        val firstResult = resizeAndEncode(file, origWidth, origHeight, orientation, 1280, 80, decorate)
        if (firstResult.dataUrl.length <= MAX_DATA_URL_LENGTH) {
            return firstResult
        }

        firstResult.bitmap.recycle()

        // Second pass: fallback 1024px, quality 70
        val fallbackResult = resizeAndEncode(file, origWidth, origHeight, orientation, 1024, 70, decorate)
        if (fallbackResult.dataUrl.length <= MAX_DATA_URL_LENGTH) {
            return fallbackResult
        }

        fallbackResult.bitmap.recycle()
        throw IllegalStateException("Foto terlalu besar setelah kompresi (maks 2,5 juta karakter). Ambil ulang foto.")
    }

    private fun resizeAndEncode(
        file: File,
        origW: Int,
        origH: Int,
        orientation: Int,
        targetLongEdge: Int,
        quality: Int,
        decorate: (Bitmap) -> Unit
    ): CompressedResult {
        val maxDim = max(origW, origH)
        var sampleSize = 1
        while (maxDim / (sampleSize * 2) >= targetLongEdge) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inMutable = true
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Gagal membaca berkas foto.")

        // Correct orientation
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }

        val oriented = if (!matrix.isIdentity) {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it != decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        // Scale to targetLongEdge if needed
        val curMax = max(oriented.width, oriented.height)
        val scaled = if (curMax > targetLongEdge) {
            val scale = targetLongEdge.toFloat() / curMax.toFloat()
            val newW = (oriented.width * scale).toInt()
            val newH = (oriented.height * scale).toInt()
            Bitmap.createScaledBitmap(oriented, newW, newH, true).also {
                if (it != oriented) oriented.recycle()
            }
        } else {
            oriented
        }

        val result = if (scaled.isMutable) scaled else scaled.copy(Bitmap.Config.ARGB_8888, true).also { scaled.recycle() }
        decorate(result)
        val outputStream = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val dataUrl = "data:image/jpeg;base64,$base64"

        return CompressedResult(
            dataUrl = dataUrl,
            width = result.width,
            height = result.height,
            bitmap = result
        )
    }
}
