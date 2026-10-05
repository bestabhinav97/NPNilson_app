package com.npnilsson.backend.config

import com.npnilsson.backend.domain.User
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

class SessionAuthenticationToken(
    val user: User,
    val token: String
) : AbstractAuthenticationToken(listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any = token
    override fun getPrincipal(): Any = user
}
