package com.npnilsson.backend.domain

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "user_sessions")
class UserSession(
    @Id
    @Column(length = 128)
    val token: String,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,

    @Column(nullable = false)
    var revoked: Boolean = false
) {
    fun isValid(now: OffsetDateTime = OffsetDateTime.now()): Boolean {
        return !revoked && expiresAt.isAfter(now)
    }
}
