package com.npnilsson.backend.exception

class InvalidCredentialsException(message: String = "Invalid email or password") : RuntimeException(message)

class UserAlreadyExistsException(message: String = "User with this email already exists") : RuntimeException(message)

class UserNotFoundException(message: String = "User not found") : RuntimeException(message)

class UnauthorizedException(message: String = "Authentication required or session expired") : RuntimeException(message)

class ForbiddenException(message: String = "Access denied") : RuntimeException(message)
