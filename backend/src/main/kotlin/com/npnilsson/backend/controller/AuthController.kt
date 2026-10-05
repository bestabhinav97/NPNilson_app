package com.npnilsson.backend.controller

import com.npnilsson.backend.config.SessionAuthenticationToken
import com.npnilsson.backend.dto.ApiResponse
import com.npnilsson.backend.dto.LoginRequest
import com.npnilsson.backend.dto.LoginResponse
import com.npnilsson.backend.dto.UserDto
import com.npnilsson.backend.exception.UnauthorizedException
import com.npnilsson.backend.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    fun getCurrentUser(): ResponseEntity<UserDto> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication is SessionAuthenticationToken) {
            val userDto = authService.getCurrentUser(authentication.token)
            return ResponseEntity.ok(userDto)
        }
        throw UnauthorizedException("Not authenticated")
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<ApiResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication is SessionAuthenticationToken) {
            authService.logout(authentication.token)
        }
        return ResponseEntity.ok(ApiResponse("Logged out successfully"))
    }
}
