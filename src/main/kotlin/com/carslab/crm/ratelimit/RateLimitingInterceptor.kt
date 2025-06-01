package com.carslab.crm.ratelimit

import com.carslab.crm.security.UserPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RateLimitingInterceptor(
    private val rateLimitingService: RateLimitingService
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val clientId = getClientIdentifier(request)
        val endpoint = request.requestURI

        val requestsPerMinute = when {
            endpoint.contains("/api/signatures") -> 10L
            endpoint.contains("/api/tablets/pair") -> 5L
            else -> 60L
        }

        if (!rateLimitingService.isAllowed("$clientId:$endpoint", requestsPerMinute)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.writer.write("""{"error": "Rate limit exceeded"}""")
            return false
        }

        return true
    }

    private fun getClientIdentifier(request: HttpServletRequest): String {
        val userPrincipal = SecurityContextHolder.getContext().authentication?.principal as? UserPrincipal
        return userPrincipal?.tenantId?.toString() ?: request.remoteAddr
    }
}