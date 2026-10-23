package com.example.np_nilson

import com.example.np_nilson.data.api.ReportCategory
import com.example.np_nilson.data.report.ReportImageLoader
import com.example.np_nilson.ui.report.ReportFormatting
import com.example.np_nilson.ui.report.ReportValidation
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class ReportValidationTest {

    @Test
    fun validReport_hasNoError() {
        assertNull(ReportValidation.validate(ReportCategory.SAFETY, "Halt golv vid entrén", photoCount = 2))
    }

    @Test
    fun missingCategory_isRejected() {
        assertEquals("Välj en kategori", ReportValidation.validate(null, "Rubrik", photoCount = 0))
    }

    @Test
    fun blankTitle_isRejected() {
        assertEquals("Ange en rubrik", ReportValidation.validate(ReportCategory.OTHER, "   ", photoCount = 0))
    }

    @Test
    fun tooLongTitle_isRejected() {
        val title = "a".repeat(ReportValidation.MAX_TITLE_LENGTH + 1)
        assertNotNull(ReportValidation.validate(ReportCategory.OTHER, title, photoCount = 0))
    }

    @Test
    fun tooManyPhotos_isRejected() {
        val error = ReportValidation.validate(ReportCategory.MAINTENANCE, "Trasig lampa", ReportValidation.MAX_PHOTOS + 1)
        assertNotNull(error)
    }

    @Test
    fun categoryApiValues_matchBackend() {
        assertEquals(
            listOf("DAMAGED_GOODS", "SAFETY", "MAINTENANCE", "OTHER"),
            ReportCategory.entries.map { it.apiValue }
        )
    }

    @Test
    fun scaledSize_keepsAspectRatioWithinMaxDimension() {
        assertEquals(1920 to 1440, ReportImageLoader.scaledSize(4000, 3000, 1920))
        assertEquals(1440 to 1920, ReportImageLoader.scaledSize(3000, 4000, 1920))
    }

    @Test
    fun scaledSize_doesNotUpscaleSmallImages() {
        assertEquals(800 to 600, ReportImageLoader.scaledSize(800, 600, 1920))
    }

    @Test
    fun sampleSize_neverShrinksBelowMaxDimension() {
        assertEquals(4, ReportImageLoader.sampleSize(1920, 1440, 300))
        assertEquals(1, ReportImageLoader.sampleSize(200, 150, 300))
    }

    @Test
    fun formatDate_usesSwedishFormatInGivenZone() {
        val formatted = ReportFormatting.formatDate("2026-10-10T20:31:00.000Z", ZoneId.of("Europe/Stockholm"))
        assertTrue(formatted, formatted.startsWith("10 okt"))
        assertTrue(formatted, formatted.endsWith("2026 22:31"))
    }

    @Test
    fun formatDate_fallsBackToRawValueWhenUnparseable() {
        assertEquals("not-a-date", ReportFormatting.formatDate("not-a-date"))
    }

    @Test
    fun labels_areSwedishAndFallBackToRawValue() {
        assertEquals("Skadat gods", ReportFormatting.categoryLabel("DAMAGED_GOODS"))
        assertEquals("Klar", ReportFormatting.statusLabel("RESOLVED"))
        assertEquals("UNKNOWN", ReportFormatting.categoryLabel("UNKNOWN"))
    }

    @Test
    fun statuses_matchBackendAndHaveSwedishLabels() {
        assertEquals(listOf("OPEN", "IN_PROGRESS", "RESOLVED"), ReportFormatting.STATUSES)
        assertEquals(listOf("Ny", "Pågår", "Klar"), ReportFormatting.STATUSES.map(ReportFormatting::statusLabel))
    }
}
