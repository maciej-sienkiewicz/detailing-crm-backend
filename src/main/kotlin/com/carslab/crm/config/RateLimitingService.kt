package com.carslab.crm.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitingService {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun isAllowed(key: String, requestsPerMinute: Long = 60): Boolean {
        val bucket = buckets.computeIfAbsent(key) { createBucket(requestsPerMinute) }
        return bucket.tryConsume(1)
    }

    private fun createBucket(requestsPerMinute: Long): Bucket {
        val limit = Bandwidth.classic(requestsPerMinute, Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)))
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }

    fun cleanupExpiredBuckets() {
        // Implement cleanup logic for old buckets
        if (buckets.size > 10000) {
            buckets.clear() // Simple cleanup - in production use LRU cache
        }
    }
}

// Rate limiting interceptor
@Component
class RateLimitingInterceptor(
    private val rateLimitingService: RateLimitingService
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val clientId = getClientIdentifier(request)
        val endpoint = request.requestURI

        val requestsPerMinute = when {
            endpoint.contains("/api/signatures") -> 10L // Lower limit for signature operations
            endpoint.contains("/api/tablets/pair") -> 5L // Very low for pairing
            else -> 60L // Default
        }

        if (!rateLimitingService.isAllowed("$clientId:$endpoint", requestsPerMinute)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.writer.write("""{"error": "Rate limit exceeded"}""")
            return false
        }

        return true
    }

    private fun getClientIdentifier(request: HttpServletRequest): String {
        // Use tenant ID if available, otherwise IP
        val userPrincipal = SecurityContextHolder.getContext().authentication?.principal as? UserPrincipal
        return userPrincipal?.tenantId?.toString() ?: request.remoteAddr
    }
}