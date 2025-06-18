// src/main/kotlin/com/carslab/crm/signature/config/CacheConfig.kt
package com.carslab.crm.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.time.Duration
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Configuration
@ConfigurationProperties(prefix = "app.signature-cache")
@Validated
data class SignatureCacheProperties(
    @field:NotNull
    @field:Min(1)
    var ttlMinutes: Long = 10,

    @field:NotNull
    @field:Min(100)
    var maxSize: Long = 1000,

    @field:NotNull
    var recordStats: Boolean = true,

    @field:NotNull
    var logRemoval: Boolean = true
)

@Configuration
class CacheConfig(
    private val cacheProperties: SignatureCacheProperties
) {

    @Bean("signatureCache")
    fun signatureCache(): Cache<String, Any> {
        val builder = Caffeine.newBuilder()
            .maximumSize(cacheProperties.maxSize)
            .expireAfterWrite(Duration.ofMinutes(cacheProperties.ttlMinutes))

        if (cacheProperties.recordStats) {
            builder.recordStats()
        }

        if (cacheProperties.logRemoval) {
            builder.removalListener<String, Any> { key, value, cause ->
                println("Cache entry removed: key=$key, cause=$cause")
            }
        }

        return builder.build()
    }
}