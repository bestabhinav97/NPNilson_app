package com.npnilsson.backend.service

import com.npnilsson.backend.domain.User
import com.npnilsson.backend.domain.UserRole
import com.npnilsson.backend.dto.CreateUserRequest
import com.npnilsson.backend.dto.UserDto
import com.npnilsson.backend.exception.UserAlreadyExistsException
import com.npnilsson.backend.exception.UserNotFoundException
import com.npnilsson.backend.repository.UserRepository
import com.npnilsson.backend.repository.UserSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userSessionRepository: UserSessionRepository,
    private val passwordService: PasswordService
) {

    @Transactional(readOnly = true)
    fun getAllUsers(): List<UserDto> {
        return userRepository.findAll().map { UserDto.fromEntity(it) }
    }

    @Transactional
    fun createUser(request: CreateUserRequest): UserDto {
        val normalizedEmail = request.email.trim().lowercase()
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw UserAlreadyExistsException("A user with email '${request.email}' already exists")
        }

        val hashedPassword = passwordService.hashPassword(request.password)

        val user = User(
            firstname = request.firstname.trim(),
            lastname = request.lastname.trim(),
            email = normalizedEmail,
            passwordHash = hashedPassword,
            role = request.role
        )

        val savedUser = userRepository.save(user)
        return UserDto.fromEntity(savedUser)
    }

    @Transactional
    fun resetPassword(userId: UUID, newPassword: String): UserDto {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User with ID $userId not found") }

        user.passwordHash = passwordService.hashPassword(newPassword)
        userSessionRepository.revokeAllUserSessions(user)
        val savedUser = userRepository.save(user)

        return UserDto.fromEntity(savedUser)
    }
}
