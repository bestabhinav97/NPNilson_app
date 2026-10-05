package com.npnilsson.backend.service

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service

@Service
class PasswordService {
    // Argon2id encoder
    private val passwordEncoder = Argon2PasswordEncoder(16, 32, 1, 65536, 3)

    fun hashPassword(password: String): String {
        return passwordEncoder.encode(password)
    }

    fun verifyPassword(rawPassword: String, encodedHash: String): Boolean {
        return passwordEncoder.matches(rawPassword, encodedHash)
    }
}
