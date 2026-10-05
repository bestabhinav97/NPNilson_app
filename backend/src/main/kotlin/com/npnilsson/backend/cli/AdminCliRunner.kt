package com.npnilsson.backend.cli

import com.npnilsson.backend.domain.UserRole
import com.npnilsson.backend.dto.CreateUserRequest
import com.npnilsson.backend.repository.UserRepository
import com.npnilsson.backend.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class AdminCliRunner(
    private val userService: UserService,
    private val userRepository: UserRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(AdminCliRunner::class.java)

    override fun run(vararg args: String) {
        if (args.isEmpty()) return

        for (i in args.indices) {
            when (args[i]) {
                "--create-user" -> {
                    if (i + 5 < args.size) {
                        val email = args[i + 1]
                        val firstname = args[i + 2]
                        val lastname = args[i + 3]
                        val password = args[i + 4]
                        val roleStr = args[i + 5]
                        val role = try { UserRole.valueOf(roleStr.uppercase()) } catch (e: Exception) { UserRole.USER }

                        try {
                            val user = userService.createUser(
                                CreateUserRequest(
                                    firstname = firstname,
                                    lastname = lastname,
                                    email = email,
                                    password = password,
                                    role = role
                                )
                            )
                            logger.info("CLI: Successfully created user ${user.email} (${user.role})")
                        } catch (e: Exception) {
                            logger.error("CLI: Failed to create user: ${e.message}")
                        }
                    } else {
                        logger.warn("CLI: Usage: --create-user <email> <firstname> <lastname> <password> <role: ADMIN|USER>")
                    }
                }
                "--reset-password" -> {
                    if (i + 2 < args.size) {
                        val email = args[i + 1]
                        val newPassword = args[i + 2]
                        val userOptional = userRepository.findByEmailIgnoreCase(email)
                        if (userOptional.isPresent) {
                            val user = userOptional.get()
                            userService.resetPassword(user.id, newPassword)
                            logger.info("CLI: Successfully reset password for user $email")
                        } else {
                            logger.error("CLI: User with email $email not found")
                        }
                    } else {
                        logger.warn("CLI: Usage: --reset-password <email> <newPassword>")
                    }
                }
            }
        }
    }
}
