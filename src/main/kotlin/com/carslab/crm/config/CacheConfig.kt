// src/main/kotlin/com/carslab/crm/config/CacheConfig.kt
package com.carslab.crm.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
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
@ConfigurationProperties(prefix = "app.vehicle-company-statistics")
@Validated
data class VehicleCompanyStatisticsCacheProperties(
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
@ConfigurationProperties(prefix = "app.visit-cache")
@Validated
data class VisitCacheProperties(
    @field:NotNull
    @field:Min(1)
    var ttlMinutes: Long = 10,

    @field:NotNull
    @field:Min(1)
    var accessTtlMinutes: Long = 5,

    @field:NotNull
    @field:Min(1000)
    var maxSize: Long = 20000,

    @field:NotNull
    var recordStats: Boolean = true,

    @field:NotNull
    var logRemoval: Boolean = false
)

@Configuration
@EnableCaching
class CacheConfig(
    private val signatureCacheProperties: SignatureCacheProperties,
    private val visitCacheProperties: VisitCacheProperties,
    private val vehicleCompanyStatisticsProperties: VehicleCompanyStatisticsCacheProperties,
    
) {
    
    @Bean("signatureCache")
    fun signatureCache(): Cache<String, Any> {
        val builder = Caffeine.newBuilder()
            .maximumSize(signatureCacheProperties.maxSize)
            .expireAfterWrite(Duration.ofMinutes(signatureCacheProperties.ttlMinutes))

        if (signatureCacheProperties.recordStats) {
            builder.recordStats()
        }

        if (signatureCacheProperties.logRemoval) {
            builder.removalListener<String, Any> { key, value, cause ->
                println("Signature cache entry removed: key=$key, cause=$cause")
            }
        }

        return builder.build()
    }

    @Bean
    @Primary
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()

        // Konfiguracja Caffeine dla cache'ów protokołów
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(visitCacheProperties.maxSize)
                .expireAfterWrite(Duration.ofMinutes(visitCacheProperties.ttlMinutes))
                .expireAfterAccess(Duration.ofMinutes(visitCacheProperties.accessTtlMinutes))
                .recordStats() // Zawsze włączone dla monitoringu
                .removalListener<Any, Any> { key, value, cause ->
                    if (visitCacheProperties.logRemoval) {
                        println("Protocol cache entry removed: key=$key, cause=$cause")
                    }
                }
        )

        cacheManager.setCacheNames(setOf(
            "visit-validation",
            "visit-company",
            "user-permissions",
            "vehicle-company-statistics"
        ))

        return cacheManager
    }

    @Bean("visitValidationCache")
    fun visitValidationCache(): Cache<String, Any> {
        val builder = Caffeine.newBuilder()
            .maximumSize(visitCacheProperties.maxSize)
            .expireAfterWrite(Duration.ofMinutes(visitCacheProperties.ttlMinutes))
            .expireAfterAccess(Duration.ofMinutes(visitCacheProperties.accessTtlMinutes))

        if (visitCacheProperties.recordStats) {
            builder.recordStats()
        }

        if (visitCacheProperties.logRemoval) {
            builder.removalListener<String, Any> { key, value, cause ->
                println("Visit validation cache entry removed: key=$key, cause=$cause")
            }
        }

        return builder.build()
    }
    
    @Bean("vehicleCompanyStatistics")
    fun vehicleCompanyStatisticsCache(): Cache<String, Any> {
        val builder = Caffeine.newBuilder()
            .maximumSize(vehicleCompanyStatisticsProperties.maxSize)
            .expireAfterWrite(Duration.ofMinutes(vehicleCompanyStatisticsProperties.ttlMinutes))

        if (vehicleCompanyStatisticsProperties.recordStats) {
            builder.recordStats()
        }

        if (vehicleCompanyStatisticsProperties.logRemoval) {
            builder.removalListener<String, Any> { key, value, cause ->
                println("VehicleCompanyStatistics cache entry removed: key=$key, cause=$cause")
            }
        }

        return builder.build()
    }
}