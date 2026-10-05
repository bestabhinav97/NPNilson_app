package com.npnilsson.backend.service

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordServiceTest {

    private val passwordService = PasswordService()

    @Test
    fun `should hash and verify password with Argon2id`() {
        val rawPassword = "SecretPassword123!"
        val hash = passwordService.hashPassword(rawPassword)

        assertTrue(hash.startsWith("\$argon2id\$"), "Hash should start with \$argon2id\$")
        assertTrue(passwordService.verifyPassword(rawPassword, hash), "Password should match hash")
        assertFalse(passwordService.verifyPassword("WrongPassword", hash), "Wrong password should fail")
    }
}
