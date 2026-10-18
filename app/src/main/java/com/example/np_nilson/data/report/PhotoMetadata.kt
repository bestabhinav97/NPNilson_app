package com.example.np_nilson.data.report

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Carries the camera's EXIF metadata (time taken, GPS position, device, camera settings)
 * over to the re-encoded upload JPEG, since Bitmap.compress writes a file without any EXIF.
 */
object PhotoMetadata {

    // Pixel dimensions and the embedded thumbnail are left out because the upload is resized
    private val COPIED_TAGS = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE
    )

    private val EXIF_DATE_TIME = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    /**
     * Returns [jpeg] with the EXIF metadata of the original photo at [source] written into it.
     * If the original has no metadata or it can't be copied, [jpeg] is returned unchanged so the
     * upload still goes through.
     */
    fun copyExif(context: Context, source: Uri, jpeg: ByteArray): ByteArray {
        val original = try {
            context.contentResolver.openInputStream(source)?.use { ExifInterface(it) }
        } catch (e: IOException) {
            null
        } ?: return jpeg

        val attributes = COPIED_TAGS
            .mapNotNull { tag -> original.getAttribute(tag)?.let { tag to it } }
            .toMap()
            .toMutableMap()
        if (attributes.isEmpty()) return jpeg

        // Without an offset the server can't tell which time zone "time taken" is in
        val takenAt = attributes[ExifInterface.TAG_DATETIME_ORIGINAL]
        if (takenAt != null && attributes[ExifInterface.TAG_OFFSET_TIME_ORIGINAL] == null) {
            offsetFor(takenAt, ZoneId.systemDefault())?.let {
                attributes[ExifInterface.TAG_OFFSET_TIME_ORIGINAL] = it
            }
        }

        // ImageDecoder (API 28+) already rotated the pixels, older devices upload them unrotated
        attributes[ExifInterface.TAG_ORIENTATION] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ExifInterface.ORIENTATION_NORMAL.toString()
        } else {
            original.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL).toString()
        }

        val file = File.createTempFile("upload_", ".jpg", context.cacheDir)
        return try {
            file.writeBytes(jpeg)
            ExifInterface(file).apply {
                attributes.forEach { (tag, value) -> setAttribute(tag, value) }
                saveAttributes()
            }
            file.readBytes()
        } catch (e: IOException) {
            jpeg
        } finally {
            file.delete()
        }
    }

    /** EXIF offset ("+02:00") that [zone] had at the EXIF local time [dateTime], or null if unparsable. */
    fun offsetFor(dateTime: String, zone: ZoneId): String? {
        val local = try {
            LocalDateTime.parse(dateTime.trim(), EXIF_DATE_TIME)
        } catch (e: DateTimeParseException) {
            return null
        }
        val offset = local.atZone(zone).offset
        // ZoneOffset.UTC prints as "Z", EXIF wants "+00:00"
        return if (offset == ZoneOffset.UTC) "+00:00" else offset.id
    }
}
