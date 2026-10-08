package com.example.np_nilson

import com.example.np_nilson.data.api.LoginRequest
import com.example.np_nilson.data.api.StoreDto
import com.example.np_nilson.data.api.UserDto
import org.junit.Assert.*
import org.junit.Test

class LoginValidationTest {

    @Test
    fun loginRequestCreation_isCorrect() {
        val request = LoginRequest(email = "admin@npnilsson.se", password = "SecretPassword123!")
        assertEquals("admin@npnilsson.se", request.email)
        assertEquals("SecretPassword123!", request.password)
    }

    @Test
    fun userDtoRole_isCorrect() {
        val user = UserDto(
            id = "123e4567-e89b-12d3-a456-426614174000",
            firstname = "NP",
            lastname = "Admin",
            email = "admin@npnilsson.se",
            role = "ADMIN"
        )
        assertEquals("ADMIN", user.role)
        assertEquals("NP", user.firstname)
        assertNull(user.store)
    }

    @Test
    fun userDtoWithStore_isCorrect() {
        val store = StoreDto(
            storeId = 1L,
            storeName = "NP Nilsson Testbutik Båstad",
            address = "Testgatan 10, Båstad"
        )
        val user = UserDto(
            id = "user-uuid-123",
            firstname = "Test",
            lastname = "Employee",
            email = "test@test.com",
            role = "USER",
            store = store
        )
        assertEquals("USER", user.role)
        assertNotNull(user.store)
        assertEquals(1L, user.store?.storeId)
        assertEquals("NP Nilsson Testbutik Båstad", user.store?.storeName)
    }

    @Test
    fun emailValidation_regexMatchesValidEmails() {
        val validEmail = "test@test.com"
        val invalidEmail = "not-an-email"

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        assertTrue(emailRegex.matches(validEmail))
        assertFalse(emailRegex.matches(invalidEmail))
    }
}
