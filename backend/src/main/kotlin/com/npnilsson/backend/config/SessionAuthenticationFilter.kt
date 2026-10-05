package com.npnilsson.backend.config

import com.npnilsson.backend.repository.UserSessionRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SessionAuthenticationFilter(
    private val userSessionRepository: UserSessionRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)
        if (!token.isNullOrBlank()) {
            val sessionOptional = userSessionRepository.findByToken(token)
            if (sessionOptional.isPresent) {
                val session = sessionOptional.get()
                if (session.isValid()) {
                    val authentication = SessionAuthenticationToken(session.user, token)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization")
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ", ignoreCase = true)) {
            return authHeader.substring(7).trim()
        }
        val xSessionHeader = request.getHeader("X-Session-Token")
        if (!xSessionHeader.isNullOrBlank()) {
            return xSessionHeader.trim()
        }
        return null
    }
}
