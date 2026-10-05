package com.npnilsson.backend.dto

import com.npnilsson.backend.domain.User
import com.npnilsson.backend.domain.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class UserDto(
    val id: UUID,
    val firstname: String,
    val lastname: String,
    val email: String,
    val role: UserRole
) {
    companion object {
        fun fromEntity(user: User): UserDto {
            return UserDto(
                id = user.id,
                firstname = user.firstname,
                lastname = user.lastname,
                email = user.email,
                role = user.role
            )
        }
    }
}

data class LoginResponse(
    val token: String,
    val user: UserDto
)

data class CreateUserRequest(
    @field:NotBlank(message = "First name is required")
    val firstname: String,

    @field:NotBlank(message = "Last name is required")
    val lastname: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    val role: UserRole = UserRole.USER
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String
)

data class ApiResponse(
    val message: String
)

data class ErrorResponse(
    val error: String
)
