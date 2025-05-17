package com.carslab.crm.infrastructure.auth

import com.carslab.crm.api.model.response.LoginResponse
import com.carslab.crm.domain.model.*
import com.carslab.crm.infrastructure.exception.BusinessException
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.persistence.entity.User
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.RoleRepository
import com.carslab.crm.infrastructure.persistence.repository.UserRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Date

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${security.jwt.secret:tojestdlugiehaslotojestdlugiehaslotojestdlugiehaslo}") private val jwtSecret: String,
    @Value("\${security.jwt.token-expiration:86400000}") private val jwtExpirationInMs: Long
) {
    fun authenticate(username: String, password: String): LoginResponse {
        val user = userRepository.findByUsername(username)
            .orElseThrow { AuthenticationException("Invalid username or password") }

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw AuthenticationException("Invalid username or password")
        }

        val token = generateToken(user)

        return LoginResponse(
            token = token,
            userId = user.id!!,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            companyId = user.companyId,
            roles = user.roles.map { it.name }
        )
    }

    private fun generateToken(user: UserEntity): String {
        val issuedAt = Date()
        val expirationDate = Date(issuedAt.time + jwtExpirationInMs)
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))

        return Jwts.builder()
            .setSubject(user.id.toString())
            .setIssuedAt(issuedAt)
            .setExpiration(expirationDate)
            .claim("username", user.username)
            .claim("companyId", user.companyId)
            .claim("roles", user.roles.map { it.name })
            .signWith(key)
            .compact()
    }

    @Transactional
    fun createUser(createUserCommand: CreateUserCommand): UserResponse {
        if (userRepository.existsByUsername(createUserCommand.username)) {
            throw BusinessException("Username '${createUserCommand.username}' is already taken")
        }

        if (userRepository.existsByEmail(createUserCommand.email)) {
            throw BusinessException("Email '${createUserCommand.email}' is already in use")
        }

        // Utwórz obiekt domeny
        val user = User(
            username = createUserCommand.username,
            passwordHash = passwordEncoder.encode(createUserCommand.password),
            email = createUserCommand.email,
            firstName = createUserCommand.firstName,
            lastName = createUserCommand.lastName,
            companyId = createUserCommand.companyId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // Konwertuj do encji i zapisz
        val userEntity = UserEntity.fromDomain(user)

        val savedUserEntity = userRepository.save(userEntity)

        return UserResponse.fromEntity(savedUserEntity)
    }

    fun getUserById(userId: Long): UserResponse {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with id: $userId") }
        return UserResponse.fromEntity(userEntity)
    }

    @Transactional
    fun assignRole(userId: Long, roleId: Long, companyId: Long) {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with id: $userId") }

        val roleEntity = roleRepository.findById(roleId)
            .orElseThrow { ResourceNotFoundException("Role not found with id: $roleId") }

        // Verify company match for security
        if (userEntity.companyId != companyId || roleEntity.companyId != companyId) {
            throw SecurityException("Cannot assign role from different company")
        }

        // Check if user already has this role
        if (userEntity.roles.any { it.id == roleId }) {
            return // Role already assigned
        }

        userEntity.addRole(roleEntity)
        userRepository.save(userEntity)
    }

    @Transactional
    fun removeRole(userId: Long, roleId: Long, companyId: Long) {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with id: $userId") }

        // Verify company match for security
        if (userEntity.companyId != companyId) {
            throw SecurityException("Cannot modify user from different company")
        }

        // Remove role
        userEntity.removeRole(roleId)
        userRepository.save(userEntity)
    }

    fun getUserRoles(userId: Long, companyId: Long): List<RoleResponse> {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with id: $userId") }

        // Verify company match for security
        if (userEntity.companyId != companyId) {
            throw SecurityException("Cannot access user from different company")
        }

        return userEntity.roles.map { RoleResponse.fromEntity(it) }
    }

    fun getUsersByCompany(companyId: Long): List<UserResponse> {
        return userRepository.findByCompanyId(companyId)
            .map { UserResponse.fromEntity(it) }
    }

    @Transactional
    fun changePassword(userId: Long, changePasswordCommand: ChangePasswordCommand): Boolean {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with id: $userId") }

        // Verify current password
        if (!passwordEncoder.matches(changePasswordCommand.currentPassword, userEntity.passwordHash)) {
            throw SecurityException("Current password is incorrect")
        }

        // Update password
        userEntity.updatePassword(passwordEncoder.encode(changePasswordCommand.newPassword))
        userRepository.save(userEntity)

        return true
    }

    @Transactional
    fun updateUserProfile(userId: Long, updateProfileCommand: UpdateProfileCommand): UserResponse {
        val userEntity = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with id: $userId") }

        // Sprawdzanie unikalności email jeśli się zmienił
        if (updateProfileCommand.email != userEntity.email &&
            userRepository.existsByEmail(updateProfileCommand.email)) {
            throw BusinessException("Email '${updateProfileCommand.email}' is already in use")
        }

        // Konwertuj do obiektu domeny
        val user = userEntity.toDomain()

        // Aktualizacja danych
        val updatedUser = user.copy(
            email = updateProfileCommand.email,
            firstName = updateProfileCommand.firstName,
            lastName = updateProfileCommand.lastName,
            updatedAt = LocalDateTime.now()
        )

        userEntity.updateFromDomain(updatedUser)
        val updatedUserEntity = userRepository.save(userEntity)

        return UserResponse.fromEntity(updatedUserEntity)
    }
}

class AuthenticationException(message: String) : RuntimeException(message)
