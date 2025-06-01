package com.carslab.crm.config

import com.carslab.crm.domain.model.PermissionDto
import com.carslab.crm.domain.permissions.PermissionService
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
import javax.crypto.SecretKey
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
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)

            if (token != null && isValidToken(token)) {
                val userId = getUserIdFromToken(token)

                val user = userRepository.findById(userId)

                user.ifPresent {
                    val auth = UsernamePasswordAuthenticationToken(it, null, getAuthorities(userId))

                    (auth as AbstractAuthenticationToken).details = getUserPermissions(userId)

                    SecurityContextHolder.getContext().authentication = auth
                    logger.debug("Set authentication for user: ${it.username} with company ID: ${it.companyId}")
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
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload

            return !isTokenExpired(claims)
        } catch (e: Exception) {
            logger.error("Invalid JWT token: ${e.message}")
            return false
        }
    }

    private fun isTokenExpired(claims: Claims): Boolean {
        return claims.expiration.before(Date())
    }

    private fun getUserIdFromToken(token: String): Long {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

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