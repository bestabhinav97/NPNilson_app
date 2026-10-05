package com.npnilsson.backend.controller

import com.npnilsson.backend.dto.CreateUserRequest
import com.npnilsson.backend.dto.ResetPasswordRequest
import com.npnilsson.backend.dto.UserDto
import com.npnilsson.backend.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val userService: UserService
) {

    @GetMapping("/users")
    fun getAllUsers(): ResponseEntity<List<UserDto>> {
        return ResponseEntity.ok(userService.getAllUsers())
    }

    @PostMapping("/users")
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserDto> {
        val newUser = userService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser)
    }

    @PostMapping("/users/{userId}/reset-password")
    fun resetPassword(
        @PathVariable userId: UUID,
        @Valid @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<UserDto> {
        val updatedUser = userService.resetPassword(userId, request.newPassword)
        return ResponseEntity.ok(updatedUser)
    }
}
