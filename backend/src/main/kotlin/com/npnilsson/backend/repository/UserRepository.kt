package com.npnilsson.backend.repository

import com.npnilsson.backend.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    fun findByEmailIgnoreCase(@Param("email") email: String): Optional<User>

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    fun existsByEmailIgnoreCase(@Param("email") email: String): Boolean
}
