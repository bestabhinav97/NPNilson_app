package com.example.np_nilson

import com.example.np_nilson.data.report.PhotoMetadata
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class PhotoMetadataTest {

    private val stockholm = ZoneId.of("Europe/Stockholm")

    @Test
    fun summerTime_inStockholm_isPlusTwo() {
        assertEquals("+02:00", PhotoMetadata.offsetFor("2026:07:15 14:30:00", stockholm))
    }

    @Test
    fun winterTime_inStockholm_isPlusOne() {
        assertEquals("+01:00", PhotoMetadata.offsetFor("2026:01:15 09:00:00", stockholm))
    }

    @Test
    fun utc_isWrittenAsPlusZero() {
        assertEquals("+00:00", PhotoMetadata.offsetFor("2026:01:15 09:00:00", ZoneId.of("UTC")))
    }

    @Test
    fun malformedDate_returnsNull() {
        assertNull(PhotoMetadata.offsetFor("2026-01-15T09:00:00", stockholm))
        assertNull(PhotoMetadata.offsetFor("", stockholm))
    }
}
