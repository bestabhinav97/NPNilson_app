package com.example.np_nilson.ui.report

import com.example.np_nilson.data.api.ReportCategory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReportFormatting {

    private val swedish = Locale.forLanguageTag("sv-SE")

    /** Report statuses in workflow order, as the backend expects them. */
    val STATUSES = listOf("OPEN", "IN_PROGRESS", "RESOLVED")

    fun categoryLabel(apiValue: String): String =
        ReportCategory.fromApiValue(apiValue)?.label ?: apiValue

    fun statusLabel(status: String): String = when (status) {
        "OPEN" -> "Öppen"
        "IN_PROGRESS" -> "Pågår"
        "RESOLVED" -> "Åtgärdad"
        else -> status
    }

    /** Formats the backend's ISO-8601 timestamp, e.g. "10 okt. 2026 22:31". */
    fun formatDate(isoTimestamp: String, zone: ZoneId = ZoneId.systemDefault()): String = try {
        DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", swedish)
            .withZone(zone)
            .format(Instant.parse(isoTimestamp))
    } catch (e: Exception) {
        isoTimestamp
    }
}
