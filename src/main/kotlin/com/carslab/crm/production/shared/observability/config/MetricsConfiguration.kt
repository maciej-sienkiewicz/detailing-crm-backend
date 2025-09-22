package com.carslab.crm.production.shared.observability.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.boot.actuator.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.Duration

@Configuration
class MetricsConfiguration {

    companion object {
        const val COMMON_TAG_COMPANY_ID = "company_id"
        const val COMMON_TAG_MODULE = "module"
        const val COMMON_TAG_OPERATION = "operation"
        const val COMMON_TAG_STATUS = "status"
        const val COMMON_TAG_ERROR_TYPE = "error_type"

        private const val MAX_CARDINALITY_PER_METER = 1000
        private const val MAX_HISTOGRAM_BUCKETS = 10
        private const val METRICS_BUFFER_SIZE = 2048
        private const val FLUSH_INTERVAL_SECONDS = 5L
    }

    @Bean
    @Primary
    fun prometheusMeterRegistry(): PrometheusMeterRegistry {
        val config = object : PrometheusConfig {
            override fun get(key: String): String? = null
            override fun step(): Duration = Duration.ofSeconds(FLUSH_INTERVAL_SECONDS)
            override fun descriptions(): Boolean = false
        }

        return PrometheusMeterRegistry(config).apply {
            config().meterFilter(MeterFilter.maximumAllowableMetrics(MAX_CARDINALITY_PER_METER))
            config().meterFilter(MeterFilter.denyNameStartsWith("jvm.gc.pause"))
            config().meterFilter(MeterFilter.denyNameStartsWith("process."))

            config().meterFilter(MeterFilter.deny { id ->
                val tags = id.tags
                tags.size > 6 || tags.any { tag ->
                    tag.key.contains("url") || tag.key.contains("path")
                }
            })
        }
    }

    @Bean
    fun commonTagsCustomizer(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config()
                .commonTags("application", "crm-production")
                .commonTags("environment", System.getProperty("spring.profiles.active", "unknown"))
                .meterFilter(MeterFilter.replaceTagValues("uri") { uri ->
                    if (uri.contains("/api/")) {
                        uri.substringBefore("?")
                            .replace(Regex("\\d+"), "{id}")
                            .replace(Regex("[a-f0-9-]{36}"), "{uuid}")
                    } else uri
                })
        }
    }

    @Bean
    fun histogramCustomizer(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config()
                .meterFilter(MeterFilter.maximumAllowableTags(
                    "http.server.requests", "uri", 100, MeterFilter.deny()
                ))
                .meterFilter(MeterFilter.maximumAllowableTags(
                    "business.operation", "operation", 200, MeterFilter.deny()
                ))
        }
    }
}