package com.carslab.crm.ratelimit

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@Service
class RateLimitingService {

    private val rateLimitCache: Cache<String, RateLimitBucket> = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build()

    fun isAllowed(key: String, requestsPerMinute: Long = 60): Boolean {
        val bucket = rateLimitCache.get(key) {
            RateLimitBucket(requestsPerMinute, System.currentTimeMillis())
        }

        return bucket.tryConsume()
    }

    private class RateLimitBucket(
        private val capacity: Long,
        private var lastRefill: Long
    ) {
        private val tokens = AtomicInteger(capacity.toInt())

        fun tryConsume(): Boolean {
            refillIfNeeded()
            return tokens.get() > 0 && tokens.decrementAndGet() >= 0
        }

        private fun refillIfNeeded() {
            val now = System.currentTimeMillis()
            if (now - lastRefill >= 60000) { // 1 minute
                tokens.set(capacity.toInt())
                lastRefill = now
            }
        }
    }
}