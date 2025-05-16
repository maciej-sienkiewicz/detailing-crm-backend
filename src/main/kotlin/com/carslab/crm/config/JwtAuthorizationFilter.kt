package com.carslab.crm.config

import com.carslab.crm.domain.PermissionService
import com.carslab.crm.domain.model.PermissionDto
import com.carslab.crm.infrastructure.persistence.repository.UserRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class JwtAuthorizationFilter(
    private val permissionService: PermissionService,
    @Value("\${security.jwt.secret}") private val jwtSecret: String,
    @Value("\${security.jwt.token-prefix:Bearer }") private val jwtTokenPrefix: String,
    @Value("\${security.jwt.token-expiration:86400000}") private val jwtExpirationInMs: Long,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(JwtAuthorizationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)

            if (token != null && isValidToken(token)) {
                val userId = getUserIdFromToken(token)

                // Pobierz pełny obiekt użytkownika
                val user = userRepository.findById(userId).orElse(null)

                if (user != null) {
                    // Ustaw pełny obiekt jako Principal
                    val auth = UsernamePasswordAuthenticationToken(
                        user,  // teraz cały obiekt użytkownika zamiast tylko ID
                        null,
                        getAuthorities(userId)
                    )

                    // Dodatkowo wciąż możesz ustawić szczegóły
                    (auth as AbstractAuthenticationToken).details = getUserPermissions(userId)

                    SecurityContextHolder.getContext().authentication = auth
                    logger.debug("Set authentication for user: ${user.username} with company ID: ${user.companyId}")
                }
            }
        } catch (e: Exception) {
            logger.error("Could not set user authentication in security context", e)
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")
        return if (header != null && header.startsWith(jwtTokenPrefix)) {
            header.substring(jwtTokenPrefix.length)
        } else null
    }

    private fun isValidToken(token: String): Boolean {
        try {
            val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            return !isTokenExpired(claims)
        } catch (e: Exception) {
            logger.error("Invalid JWT token: {${e.message}}")
            return false
        }
    }

    private fun isTokenExpired(claims: Claims): Boolean {
        return claims.expiration.before(Date())
    }

    private fun getUserIdFromToken(token: String): Long {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        return claims.subject.toLong()
    }

    private fun getAuthorities(userId: Long): List<SimpleGrantedAuthority> {
        val permissions = permissionService.getUserPermissions(userId)

        return permissions.map { permission ->
            SimpleGrantedAuthority("${permission.resourceType}_${permission.action}")
        }.plus(
            // Add role-based authorities for backward compatibility
            permissionService.getUserRoles(userId).map { role ->
                SimpleGrantedAuthority("ROLE_${role}")
            }
        )
    }

    private fun getUserPermissions(userId: Long): List<PermissionDto> {
        return permissionService.getUserPermissions(userId)
    }
}