package com.npnilsson.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.npnilsson.backend.dto.LoginRequest
import com.npnilsson.backend.dto.LoginResponse
import com.npnilsson.backend.dto.UserDto
import com.npnilsson.backend.domain.UserRole
import com.npnilsson.backend.exception.InvalidCredentialsException
import com.npnilsson.backend.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var authService: AuthService

    @Test
    fun `login with valid credentials should return token and user info`() {
        val loginRequest = LoginRequest("admin@npnilsson.se", "AdminSecurePassword123!")
        val userDto = UserDto(UUID.randomUUID(), "NP", "Admin", "admin@npnilsson.se", UserRole.ADMIN)
        val loginResponse = LoginResponse("mock-token-123", userDto)

        given(authService.login(loginRequest)).willReturn(loginResponse)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("mock-token-123"))
            .andExpect(jsonPath("$.user.email").value("admin@npnilsson.se"))
            .andExpect(jsonPath("$.user.role").value("ADMIN"))
    }

    @Test
    fun `login with invalid credentials should return generic error`() {
        val loginRequest = LoginRequest("wrong@npnilsson.se", "wrongpassword")

        given(authService.login(loginRequest)).willThrow(InvalidCredentialsException("Invalid email or password"))

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("Invalid email or password"))
    }
}
