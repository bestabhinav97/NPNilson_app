package com.example.np_nilson.data.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Loads report photos from a content Uri (camera or gallery), downscaled so that
 * neither side exceeds [maxDimension]. Run off the main thread.
 */
object ReportImageLoader {

    const val UPLOAD_MAX_DIMENSION = 1920
    private const val UPLOAD_JPEG_QUALITY = 80

    fun loadBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies EXIF rotation, so camera photos come out upright
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val (width, height) = scaledSize(info.size.width, info.size.height, maxDimension)
                decoder.setTargetSize(width, height)
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IllegalArgumentException("Could not decode image $uri")
        }
    }

    /** Decodes downloaded image bytes, downscaled so neither side exceeds [maxDimension]. */
    fun decodeBytes(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    /** Re-encodes the photo as a downscaled JPEG so uploads stay small, keeping its EXIF metadata. */
    fun compressForUpload(context: Context, uri: Uri): ByteArray {
        val bitmap = loadBitmap(context, uri, UPLOAD_MAX_DIMENSION)
        val jpeg = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, out)
            bitmap.recycle()
            out.toByteArray()
        }
        return PhotoMetadata.copyExif(context, uri, jpeg)
    }

    /** Largest power-of-two subsampling that keeps the longest side at or above [maxDimension]. */
    fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (max(width, height) / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    fun scaledSize(width: Int, height: Int, maxDimension: Int): Pair<Int, Int> {
        val largest = max(width, height)
        if (largest <= maxDimension) return width to height
        val scale = maxDimension.toFloat() / largest
        return max(1, (width * scale).toInt()) to max(1, (height * scale).toInt())
    }
}
