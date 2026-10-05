package com.example.np_nilson

import com.example.np_nilson.data.api.LoginRequest
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
    }

    @Test
    fun emailValidation_regexMatchesValidEmails() {
        val validEmail = "test@npnilsson.se"
        val invalidEmail = "not-an-email"

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        assertTrue(emailRegex.matches(validEmail))
        assertFalse(emailRegex.matches(invalidEmail))
    }
}
