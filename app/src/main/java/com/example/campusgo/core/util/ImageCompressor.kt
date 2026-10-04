package com.example.campusgo.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {

    /**
     * Comprime y optimiza imágenes de forma altamente resiliente para cualquier dispositivo
     * Android (Samsung OneUI, Xiaomi MIUI/HyperOS, Motorola, Pixel, etc.).
     *
     * Lee el flujo content:// en una sola pasada para evitar que el ContentResolver cierre
     * o revoque el permiso del stream, y garantiza un fallback seguro a bytes crudos si la
     * decodificación de mapa de bits falla.
     */
    suspend fun compressImageFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1280,
        maxHeight: Int = 1280,
        quality: Int = 80
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            // 1. Leer bytes una sola vez del ContentResolver
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            } ?: return@withContext Result.failure(IllegalStateException("No se pudo abrir el archivo de imagen"))

            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Archivo de imagen vacío"))
            }

            // 2. Analizar dimensiones desde el buffer en memoria
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

            val originalWidth = boundsOptions.outWidth
            val originalHeight = boundsOptions.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                // Si no se pueden obtener dimensiones (ej. formato exótico), devolver bytes originales
                return@withContext Result.success(rawBytes)
            }

            // 3. Submuestreo seguro con ARGB_8888 (compatible con canales alfa / PNG)
            val sampleSize = calculateInSampleSize(boundsOptions, maxWidth, maxHeight)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return@withContext Result.success(rawBytes)

            // 4. Escalar proporcionalmente si excede los límites máximos
            val width = decodedBitmap.width
            val height = decodedBitmap.height
            val finalBitmap = if (width > maxWidth || height > maxHeight) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW = if (ratio > 1f) maxWidth else (maxHeight * ratio).toInt()
                val targetH = if (ratio > 1f) (maxWidth / ratio).toInt() else maxHeight
                Bitmap.createScaledBitmap(decodedBitmap, max(1, targetW), max(1, targetH), true)
            } else {
                decodedBitmap
            }

            // 5. Comprimir a JPEG
            val outputStream = ByteArrayOutputStream()
            val compressed = finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            if (finalBitmap != decodedBitmap && !decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
            if (!finalBitmap.isRecycled) {
                finalBitmap.recycle()
            }

            if (compressed && outputStream.size() > 0) {
                Result.success(outputStream.toByteArray())
            } else {
                Result.success(rawBytes)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error procesando imagen: ${e.message}", e)
            try {
                val fallbackBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (fallbackBytes != null && fallbackBytes.isNotEmpty()) {
                    Result.success(fallbackBytes)
                } else {
                    Result.failure(e)
                }
            } catch (fallbackEx: Exception) {
                Result.failure(fallbackEx)
            }
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }
}
