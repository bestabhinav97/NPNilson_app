package com.npnilsson.backend.service

import com.npnilsson.backend.domain.UserSession
import com.npnilsson.backend.dto.LoginRequest
import com.npnilsson.backend.dto.LoginResponse
import com.npnilsson.backend.dto.UserDto
import com.npnilsson.backend.exception.InvalidCredentialsException
import com.npnilsson.backend.exception.UnauthorizedException
import com.npnilsson.backend.repository.UserRepository
import com.npnilsson.backend.repository.UserSessionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.HexFormat
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userSessionRepository: UserSessionRepository,
    private val passwordService: PasswordService,
    @Value("\${app.session.expiration-hours:24}") private val sessionExpirationHours: Long
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val normalizedEmail = request.email.trim().lowercase()
        val user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow { InvalidCredentialsException("Invalid email or password") }

        if (!passwordService.verifyPassword(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid email or password")
        }

        val tokenBytes = ByteArray(32)
        secureRandom.nextBytes(tokenBytes)
        val token = HexFormat.of().formatHex(tokenBytes)

        val session = UserSession(
            token = token,
            user = user,
            expiresAt = OffsetDateTime.now().plusHours(sessionExpirationHours)
        )
        userSessionRepository.save(session)

        return LoginResponse(
            token = token,
            user = UserDto.fromEntity(user)
        )
    }

    @Transactional(readOnly = true)
    fun getCurrentUser(token: String): UserDto {
        val session = userSessionRepository.findByToken(token)
            .orElseThrow { UnauthorizedException("Session expired or invalid") }

        if (!session.isValid()) {
            throw UnauthorizedException("Session expired or invalid")
        }

        return UserDto.fromEntity(session.user)
    }

    @Transactional
    fun logout(token: String) {
        val sessionOptional = userSessionRepository.findByToken(token)
        if (sessionOptional.isPresent) {
            val session = sessionOptional.get()
            session.revoked = true
            userSessionRepository.save(session)
        }
    }
}
