package com.npnilsson.backend.repository

import com.npnilsson.backend.domain.User
import com.npnilsson.backend.domain.UserSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserSessionRepository : JpaRepository<UserSession, String> {
    fun findByToken(token: String): Optional<UserSession>

    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user = :user")
    fun revokeAllUserSessions(@Param("user") user: User)
}
