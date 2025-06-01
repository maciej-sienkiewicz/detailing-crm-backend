package com.carslab.crm.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.crypto.SecretKey
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class JwtService(
    @Value("\${security.jwt.secret:tojestdlugiehaslotojestdlugiehaslotojestdlugiehaslo}")
    private val jwtSecret: String,

    @Value("\${security.jwt.token-expiration:86400000}")
    private val jwtExpirationInMs: Long,

    @Value("\${app.jwt.expiration:3600}")
    private val deviceExpirationInSeconds: Long
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))

    /**
     * Generuj token dla użytkownika (kompatybilny z istniejącym UserService)
     */
    fun generateUserToken(
        userId: Long,
        username: String,
        email: String? = null,
        companyId: Long,
        roles: List<String>
    ): String {
        val issuedAt = Date()
        val expirationDate = Date(issuedAt.time + jwtExpirationInMs)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(issuedAt)
            .expiration(expirationDate)
            .issuer("crm-system")
            .claim("username", username)
            .claim("email", email)
            .claim("companyId", companyId)
            .claim("roles", roles)
            .claim("permissions", getPermissionsForRoles(roles))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact()
    }

    /**
     * Generuj token dla tabletu (używa UUID i tenantId)
     */
    fun generateTabletToken(deviceId: UUID, tenantId: UUID): String {
        val now = Instant.now()
        val expirationDate = Date.from(now.plus(deviceExpirationInSeconds * 24, ChronoUnit.SECONDS))

        return Jwts.builder()
            .subject(deviceId.toString())
            .issuedAt(Date.from(now))
            .expiration(expirationDate)
            .issuer("crm-system")
            .claim("tenant_id", tenantId.toString())
            .claim("device_type", "tablet")
            .claim("roles", listOf("TABLET"))
            .claim("permissions", listOf("signature:submit", "websocket:connect"))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact()
    }

    /**
     * Waliduj token (backward compatible)
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .payload

            // BACKWARD COMPATIBILITY: issuer opcjonalny
            val issuer = claims.issuer
            if (issuer != null && issuer != "crm-system") {
                logger.warn("Invalid JWT token: Wrong issuer '$issuer', expected 'crm-system'")
                return false
            }

            // Sprawdź wygaśnięcie
            if (claims.expiration.before(Date())) {
                logger.warn("JWT token expired")
                return false
            }

            true
        } catch (ex: ExpiredJwtException) {
            logger.warn("JWT token expired: ${ex.message}")
            false
        } catch (ex: Exception) {
            logger.error("JWT token validation error: ${ex.message}")
            false
        }
    }

    /**
     * Wyciągnij claims dla użytkownika (kompatybilne z istniejącym systemem)
     */
    fun extractUserClaims(token: String): UserTokenClaims {
        val claims = extractAllClaims(token)

        return UserTokenClaims(
            userId = claims.subject.toLongOrNull() ?: throw IllegalArgumentException("Invalid user ID in token"),
            username = claims["username"] as? String ?: "",
            email = claims["email"] as? String,
            companyId = (claims["companyId"] as? Number)?.toLong()
                ?: throw IllegalArgumentException("Missing companyId in token"),
            roles = claims["roles"] as? List<String> ?: emptyList(),
            permissions = claims["permissions"] as? List<String> ?: emptyList()
        )
    }

    /**
     * Wyciągnij claims dla tabletu
     */
    fun extractTabletClaims(token: String): TabletTokenClaims {
        val claims = extractAllClaims(token)

        return TabletTokenClaims(
            deviceId = UUID.fromString(claims.subject),
            tenantId = UUID.fromString(claims["tenant_id"] as String),
            deviceType = claims["device_type"] as? String ?: "tablet",
            roles = claims["roles"] as? List<String> ?: emptyList(),
            permissions = claims["permissions"] as? List<String> ?: emptyList()
        )
    }

    /**
     * Sprawdź typ tokenu (user vs tablet)
     */
    fun getTokenType(token: String): TokenType {
        return try {
            val claims = extractAllClaims(token)
            when {
                claims["device_type"] != null -> TokenType.TABLET
                claims["username"] != null -> TokenType.USER
                else -> TokenType.UNKNOWN
            }
        } catch (e: Exception) {
            TokenType.UNKNOWN
        }
    }

    /**
     * Uniwersalna metoda do wyciągnięcia claims
     */
    fun extractClaims(token: String): Any {
        return when (getTokenType(token)) {
            TokenType.USER -> extractUserClaims(token)
            TokenType.TABLET -> extractTabletClaims(token)
            TokenType.UNKNOWN -> throw IllegalArgumentException("Unknown token type")
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun getPermissionsForRoles(roles: List<String>): List<String> {
        val permissions = mutableListOf<String>()

        roles.forEach { role ->
            when (role.uppercase()) {
                "USER" -> permissions.addAll(listOf("signature:request", "signature:read"))
                "ADMIN" -> permissions.addAll(listOf("signature:*", "tablet:*", "workstation:*", "user:*"))
                "MANAGER" -> permissions.addAll(listOf("signature:*", "tablet:manage", "user:read"))
                "TABLET" -> permissions.addAll(listOf("signature:submit", "websocket:connect"))
                "EMPLOYEE" -> permissions.addAll(listOf("signature:request", "signature:read"))
            }
        }

        return permissions.distinct()
    }

    /**
     * Debug metoda
     */
    fun debugToken(token: String): Map<String, Any?> {
        return try {
            val claims = extractAllClaims(token)
            mapOf(
                "subject" to claims.subject,
                "issuer" to claims.issuer,
                "issuedAt" to claims.issuedAt,
                "expiration" to claims.expiration,
                "username" to claims["username"],
                "email" to claims["email"],
                "companyId" to claims["companyId"],
                "tenant_id" to claims["tenant_id"],
                "device_type" to claims["device_type"],
                "roles" to claims["roles"],
                "permissions" to claims["permissions"],
                "isExpired" to claims.expiration.before(Date()),
                "tokenType" to getTokenType(token).name
            )
        } catch (e: Exception) {
            mapOf("error" to e.message)
        }
    }
}

// Data classes
data class UserTokenClaims(
    val userId: Long,
    val username: String,
    val email: String?,
    val companyId: Long,
    val roles: List<String>,
    val permissions: List<String>
)

data class TabletTokenClaims(
    val deviceId: UUID,
    val tenantId: UUID,
    val deviceType: String,
    val roles: List<String>,
    val permissions: List<String>
)

enum class TokenType {
    USER, TABLET, UNKNOWN
}