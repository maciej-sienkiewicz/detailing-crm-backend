package com.carslab.crm.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * UserPrincipal kompatybilny z istniejącym systemem użytkowników
 */
data class UserPrincipal(
    val id: Long,                    // Long zamiast UUID dla kompatybilności
    val userUsername: String,        // Zmienione z 'username' żeby uniknąć konfliktu
    val email: String?,
    val companyId: Long,
    private val grantedAuthorities: Collection<GrantedAuthority>
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = grantedAuthorities
    override fun getPassword(): String? = null
    override fun getUsername(): String = userUsername  // Implementacja interfejsu UserDetails
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    // Convenience methods
    fun hasRole(role: String): Boolean {
        return authorities.any { it.authority == "ROLE_${role.uppercase()}" }
    }

    fun hasPermission(permission: String): Boolean {
        return authorities.any { it.authority == permission }
    }

    companion object {
        /**
         * Tworzenie z UserTokenClaims (dla user tokenów)
         */
        fun fromUserTokenClaims(claims: UserTokenClaims): UserPrincipal {
            val authorities = mutableListOf<GrantedAuthority>()

            // Dodaj role z prefiksem ROLE_
            claims.roles.forEach { role ->
                authorities.add(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
            }

            // Dodaj permissions
            claims.permissions.forEach { permission ->
                authorities.add(SimpleGrantedAuthority(permission))
            }

            return UserPrincipal(
                id = claims.userId,
                userUsername = claims.username,
                email = claims.email,
                companyId = claims.companyId,
                grantedAuthorities = authorities
            )
        }

        /**
         * Tworzenie z TabletTokenClaims (dla tablet tokenów)
         */
        fun fromTabletTokenClaims(claims: TabletTokenClaims): UserPrincipal {
            val authorities = mutableListOf<GrantedAuthority>()

            // Dodaj role z prefiksem ROLE_
            claims.roles.forEach { role ->
                authorities.add(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
            }

            // Dodaj permissions
            claims.permissions.forEach { permission ->
                authorities.add(SimpleGrantedAuthority(permission))
            }

            return UserPrincipal(
                id = -1L, // Placeholder dla tabletów
                userUsername = "tablet-${claims.deviceId}",
                email = null,
                companyId = -1L,
                grantedAuthorities = authorities
            )
        }
    }
}

/**
 * Tablet Principal dla urządzeń
 */
data class TabletPrincipal(
    val deviceId: java.util.UUID,
    val companyId: Long,
    val deviceType: String,
    private val grantedAuthorities: Collection<GrantedAuthority>
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = grantedAuthorities
    override fun getPassword(): String? = null
    override fun getUsername(): String = "tablet-$deviceId"
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    companion object {
        fun fromTabletTokenClaims(claims: TabletTokenClaims): TabletPrincipal {
            val authorities = mutableListOf<GrantedAuthority>()

            claims.roles.forEach { role ->
                authorities.add(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
            }

            claims.permissions.forEach { permission ->
                authorities.add(SimpleGrantedAuthority(permission))
            }

            return TabletPrincipal(
                deviceId = claims.deviceId,
                companyId = claims.companyId,
                deviceType = claims.deviceType,
                grantedAuthorities = authorities
            )
        }
    }
}