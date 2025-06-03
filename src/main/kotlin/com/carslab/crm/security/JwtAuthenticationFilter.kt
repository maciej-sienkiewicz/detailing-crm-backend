package com.carslab.crm.security

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // ZAWSZE pozwól na OPTIONS requests (preflight)
            if ("OPTIONS".equals(request.method, ignoreCase = true)) {
                logger.debug("Allowing OPTIONS request for: ${request.requestURI}")
                filterChain.doFilter(request, response)
                return
            }

            val authHeader = request.getHeader("Authorization")

            // Skip JWT validation for public endpoints
            if (isPublicEndpoint(request.requestURI)) {
                logger.debug("Skipping JWT validation for public endpoint: ${request.requestURI}")
                filterChain.doFilter(request, response)
                return
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.debug("No Authorization header or doesn't start with Bearer for ${request.requestURI}")
                filterChain.doFilter(request, response)
                return
            }

            val token = authHeader.substring(7)

            if (jwtService.validateToken(token)) {
                try {
                    // Sprawdź typ tokenu i utwórz odpowiedni principal
                    when (jwtService.getTokenType(token)) {
                        TokenType.USER -> {
                            val userClaims = jwtService.extractUserClaims(token)
                            val userPrincipal = UserPrincipal.fromUserTokenClaims(userClaims)

                            val authentication = UsernamePasswordAuthenticationToken(
                                userPrincipal,
                                null,
                                userPrincipal.authorities
                            )

                            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                            SecurityContextHolder.getContext().authentication = authentication

                            logger.debug("Successfully authenticated user: ${userClaims.username} for ${request.requestURI}")
                        }

                        TokenType.TABLET -> {
                            val tabletClaims = jwtService.extractTabletClaims(token)
                            val tabletPrincipal = TabletPrincipal.fromTabletTokenClaims(tabletClaims)

                            val authentication = UsernamePasswordAuthenticationToken(
                                tabletPrincipal,
                                null,
                                tabletPrincipal.authorities
                            )

                            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                            SecurityContextHolder.getContext().authentication = authentication

                            logger.debug("Successfully authenticated tablet: ${tabletClaims.deviceId} for ${request.requestURI}")
                        }

                        TokenType.UNKNOWN -> {
                            logger.warn("Unknown token type for ${request.requestURI}")
                            SecurityContextHolder.clearContext()
                        }
                    }

                } catch (e: Exception) {
                    logger.warn("Error extracting claims from valid token for ${request.requestURI}: ${e.message}")
                    SecurityContextHolder.clearContext()
                }
            } else {
                logger.debug("Invalid JWT token for ${request.requestURI}")

                // Debug info tylko w trybie DEBUG
                if (logger.isDebugEnabled) {
                    val tokenPreview = if (token.length > 20) {
                        "${token.take(10)}...${token.takeLast(10)}"
                    } else {
                        "short_token"
                    }
                    logger.debug("Token preview: $tokenPreview")

                    // Debug token details
                    try {
                        val debugInfo = jwtService.debugToken(token)
                        logger.debug("Token debug info: $debugInfo")
                    } catch (e: Exception) {
                        logger.debug("Cannot parse token for debugging: ${e.message}")
                    }
                }
            }

        } catch (e: Exception) {
            logger.error("JWT Authentication error for ${request.requestURI}: ${e.message}", e)
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }

    private fun isPublicEndpoint(uri: String): Boolean {
        val publicPaths = listOf(
            "/api/auth/login",
            "/api/auth/register",
            "/api/tablets/auth",
            "/api/tablets/register",
            "/api/tablets/pair",
            "/api/debug/token",
            "/api/health",
            "/api/users",
            "/actuator",
            "/error",
            "/favicon.ico",
            "/ws/"
        )
        return publicPaths.any { uri.startsWith(it) }
    }
}