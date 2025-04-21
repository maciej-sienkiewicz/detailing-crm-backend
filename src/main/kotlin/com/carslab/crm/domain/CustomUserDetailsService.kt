package com.carslab.crm.domain

import com.carslab.crm.infrastructure.persistence.repository.PermissionConfigurationRepository
import com.carslab.crm.infrastructure.persistence.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
    private val permissionConfigurationRepository: PermissionConfigurationRepository,
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User not found with username: $username") }

        val authorities = user.roles.flatMap { role ->
            role.permissions
                .filter { permission ->
                    val config = permissionConfigurationRepository.findByRoleIdAndPermissionId(role.id!!, permission.id!!)
                    config?.enabled ?: false
                }
                .map { permission ->
                    SimpleGrantedAuthority("${permission.resourceType}_${permission.action}")
                }
        }.toMutableList()

        // Dodaj role jako authority (dla kompatybilności z hasRole w Security)
        user.roles.forEach { role ->
            authorities.add(SimpleGrantedAuthority("ROLE_${role.name}"))
        }

        return org.springframework.security.core.userdetails.User
            .withUsername(user.username)
            .password(user.passwordHash)
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build()
    }
}