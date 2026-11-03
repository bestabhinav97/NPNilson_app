package com.example.np_nilson

import com.example.np_nilson.data.api.CategoryFieldDto
import com.example.np_nilson.ui.report.DynamicFieldValidation
import org.junit.Assert.*
import org.junit.Test

class DynamicFieldValidationTest {
    @Test fun requiredTextRejectsWhitespace() {
        val field = CategoryFieldDto(label = "Plats", required = true)
        assertThrows(IllegalArgumentException::class.java) { DynamicFieldValidation.values(listOf(field), mapOf(field.id to "  ")) }
    }
    @Test fun numbersSupportSwedishDecimalsAndLimits() {
        val field = CategoryFieldDto(label = "Antal", type = "NUMBER", validation = mapOf("min" to -2.0, "max" to 10.0))
        assertEquals(1.5, DynamicFieldValidation.values(listOf(field), mapOf(field.id to "1,5"))[field.id])
        for (bad in listOf("NaN","Infinity","11","-3","abc")) {
            assertThrows(IllegalArgumentException::class.java) { DynamicFieldValidation.values(listOf(field), mapOf(field.id to bad)) }
        }
    }
    @Test fun datesRejectImpossibleDays() {
        val field = CategoryFieldDto(type = "DATE", label = "Datum")
        assertThrows(IllegalArgumentException::class.java) { DynamicFieldValidation.values(listOf(field), mapOf(field.id to "2026-02-30")) }
        assertEquals("2026-10-26", DynamicFieldValidation.values(listOf(field),mapOf(field.id to "2026-10-26"))[field.id])
    }
    @Test fun falseIsAValidRequiredCheckboxAnswer() {
        val field = CategoryFieldDto(type = "CHECKBOX", required = true, label = "Stoppad")
        assertEquals(false, DynamicFieldValidation.values(listOf(field),mapOf(field.id to "false"))[field.id])
    }
    @Test fun dropdownCannotSubmitRemovedOption() {
        val field = CategoryFieldDto(type = "SELECT", options = listOf("A","B"))
        assertThrows(IllegalArgumentException::class.java) { DynamicFieldValidation.values(listOf(field),mapOf(field.id to "C")) }
    }
    @Test fun optionalFieldsAreOmittedAndTextLimitsApply() {
        val field = CategoryFieldDto(validation = mapOf("maxLength" to 3.0))
        assertTrue(DynamicFieldValidation.values(listOf(field), emptyMap()).isEmpty())
        assertThrows(IllegalArgumentException::class.java) { DynamicFieldValidation.values(listOf(field),mapOf(field.id to "long")) }
    }
}
