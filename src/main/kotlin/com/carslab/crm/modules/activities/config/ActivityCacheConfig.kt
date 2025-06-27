package com.carslab.crm.modules.activities.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class ActivityCacheConfig {

    @Bean("activitySummaryCache")
    fun activitySummaryCache(): Cache<String, Any> {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats()
            .removalListener<String, Any> { key, value, cause ->
                println("Activity summary cache entry removed: key=$key, cause=$cause")
            }
            .build()
    }

    @Bean("activityAnalyticsCache")
    fun activityAnalyticsCache(): Cache<String, Any> {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .removalListener<String, Any> { key, value, cause ->
                println("Activity analytics cache entry removed: key=$key, cause=$cause")
            }
            .build()
    }
}