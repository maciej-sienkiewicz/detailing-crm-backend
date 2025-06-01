package com.carslab.crm.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.crypto.SecretKey
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
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateTabletToken(deviceId: UUID, tenantId: UUID): String {
        val now = Instant.now()

        return Jwts.builder()
            .subject(deviceId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(jwtExpirationInSeconds * 24, ChronoUnit.SECONDS))) // 24h for tablets
            .issuer("crm-system")
            .claim("tenant_id", tenantId.toString())
            .claim("device_type", "tablet")
            .claim("roles", listOf("TABLET"))
            .claim("permissions", listOf("signature:submit", "websocket:connect"))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact()
    }

    fun generateUserToken(userId: UUID, tenantId: UUID, email: String, roles: List<String>): String {
        val now = Instant.now()

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(jwtExpirationInSeconds, ChronoUnit.SECONDS)))
            .issuer("crm-system")
            .claim("tenant_id", tenantId.toString())
            .claim("email", email)
            .claim("roles", roles)
            .claim("permissions", getPermissionsForRoles(roles))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer("crm-system")
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
            true
        } catch (ex: Exception) {
            logger.error("Invalid JWT token: ${ex.message}")
            false
        }
    }

    fun extractClaims(token: String): JwtClaims {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

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