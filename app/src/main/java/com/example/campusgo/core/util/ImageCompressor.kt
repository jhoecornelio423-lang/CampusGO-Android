package com.example.campusgo.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageCompressor {

    /**
     * Comprime y optimiza imágenes seleccionadas desde la galería o cámara de forma segura
     * para cualquier dispositivo Android (Samsung OneUI, Xiaomi MIUI/HyperOS, Motorola, etc.).
     *
     * Previene OutOfMemoryError (OOM) en fotos de alta resolución (48MP/108MP) y
     * corrige automáticamente la orientación EXIF en dispositivos Samsung.
     */
    suspend fun compressImageFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1280,
        maxHeight: Int = 1280,
        quality: Int = 80
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            // 1. Obtener orientación EXIF si está disponible (crucial en Samsung Galaxy)
            val orientation = getExifOrientation(context, uri)

            // 2. Primera pasada: leer sólo dimensiones sin alojar mapa de bits en memoria
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext Result.failure(IllegalStateException("No se pudo abrir el archivo de imagen"))

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Dimensiones de imagen inválidas"))
            }

            // 3. Calcular factor de reducción de escala (submuestreo exponencial)
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Optimización de memoria RAM

            // 4. Segunda pasada: decodificar mapa de bits ya reducido
            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext Result.failure(IllegalStateException("Fallo al decodificar la imagen"))

            // 5. Aplicar rotación EXIF y escalado final si es necesario
            val finalBitmap = adjustBitmapOrientationAndScale(decodedBitmap, orientation, maxWidth, maxHeight)

            // 6. Comprimir a formato JPEG
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            if (finalBitmap != decodedBitmap && !decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
            if (!finalBitmap.isRecycled) {
                finalBitmap.recycle()
            }

            val compressedBytes = outputStream.toByteArray()
            Result.success(compressedBytes)
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error al procesar imagen: ${e.message}", e)
            Result.failure(e)
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

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun adjustBitmapOrientationAndScale(
        bitmap: Bitmap,
        orientation: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }

        val rotatedBitmap = if (!matrix.isIdentity) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        // Si excede el tamaño máximo, escalar proporcionalmente
        val width = rotatedBitmap.width
        val height = rotatedBitmap.height
        if (width > maxWidth || height > maxHeight) {
            val ratio = width.toFloat() / height.toFloat()
            val targetWidth: Int
            val targetHeight: Int
            if (ratio > 1) {
                targetWidth = maxWidth
                targetHeight = (maxWidth / ratio).toInt()
            } else {
                targetHeight = maxHeight
                targetWidth = (maxHeight * ratio).toInt()
            }
            val scaled = Bitmap.createScaledBitmap(rotatedBitmap, targetWidth, targetHeight, true)
            if (rotatedBitmap != bitmap && !rotatedBitmap.isRecycled) {
                rotatedBitmap.recycle()
            }
            return scaled
        }

        return rotatedBitmap
    }
}
