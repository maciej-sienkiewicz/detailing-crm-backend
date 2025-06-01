package com.carslab.crm.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class JwtService(
    @Value("\${app.jwt.secret:your-very-long-secret-key-here-change-in-production-minimum-256-bits}")
    private val jwtSecret: String,
    @Value("\${app.jwt.expiration:3600}")
    private val jwtExpirationInSeconds: Long
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val key: Key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateTabletToken(deviceId: UUID, tenantId: UUID): String {
        val now = Instant.now()

        return Jwts.builder()
            .setSubject(deviceId.toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(jwtExpirationInSeconds * 24, ChronoUnit.SECONDS))) // 24h for tablets
            .setIssuer("crm-system")
            .claim("tenant_id", tenantId.toString())
            .claim("device_type", "tablet")
            .claim("roles", listOf("TABLET"))
            .claim("permissions", listOf("signature:submit", "websocket:connect"))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    fun generateUserToken(userId: UUID, tenantId: UUID, email: String, roles: List<String>): String {
        val now = Instant.now()

        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(jwtExpirationInSeconds, ChronoUnit.SECONDS)))
            .setIssuer("crm-system")
            .claim("tenant_id", tenantId.toString())
            .claim("email", email)
            .claim("roles", roles)
            .claim("permissions", getPermissionsForRoles(roles))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer("crm-system")
                .setAllowedClockSkewSeconds(30)
                .build()
                .parseClaimsJws(token)
            true
        } catch (ex: Exception) {
            logger.error("Invalid JWT token: ${ex.message}")
            false
        }
    }

    fun extractClaims(token: String): JwtClaims {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        return JwtClaims(
            userId = UUID.fromString(claims.subject),
            tenantId = UUID.fromString(claims["tenant_id"].toString()),
            email = claims["email"]?.toString() ?: "",
            roles = claims["roles"] as List<String>,
            permissions = claims["permissions"] as? List<String> ?: emptyList()
        )
    }

    private fun getPermissionsForRoles(roles: List<String>): List<String> {
        val permissions = mutableListOf<String>()

        roles.forEach { role ->
            when (role) {
                "USER" -> permissions.addAll(listOf("signature:request", "signature:read"))
                "ADMIN" -> permissions.addAll(listOf("signature:*", "tablet:*", "workstation:*"))
                "MANAGER" -> permissions.addAll(listOf("signature:*", "tablet:manage"))
                "TABLET" -> permissions.addAll(listOf("signature:submit", "websocket:connect"))
            }
        }

        return permissions
    }
}

data class JwtClaims(
    val userId: UUID,
    val tenantId: UUID,
    val email: String,
    val roles: List<String>,
    val permissions: List<String>
)