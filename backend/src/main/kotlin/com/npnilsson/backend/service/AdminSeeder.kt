package com.npnilsson.backend.service

import com.npnilsson.backend.domain.User
import com.npnilsson.backend.domain.UserRole
import com.npnilsson.backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminSeeder(
    private val userRepository: UserRepository,
    private val passwordService: PasswordService,
    @Value("\${app.admin.seed.email:admin@npnilsson.se}") private val seedEmail: String,
    @Value("\${app.admin.seed.password:AdminSecurePassword123!}") private val seedPassword: String,
    @Value("\${app.admin.seed.firstname:NP}") private val seedFirstname: String,
    @Value("\${app.admin.seed.lastname:Admin}") private val seedLastname: String
) {
    private val logger = LoggerFactory.getLogger(AdminSeeder::class.java)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun seedAdmin() {
        val normalizedEmail = seedEmail.trim().lowercase()
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            logger.info("Admin user with email '$normalizedEmail' already exists. Skipping seed.")
            return
        }

        logger.info("Seeding initial admin user with email '$normalizedEmail'")
        val adminUser = User(
            firstname = seedFirstname,
            lastname = seedLastname,
            email = normalizedEmail,
            passwordHash = passwordService.hashPassword(seedPassword),
            role = UserRole.ADMIN
        )
        userRepository.save(adminUser)
        logger.info("Successfully seeded admin user.")
    }
}
