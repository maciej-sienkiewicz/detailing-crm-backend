package com.carslab.crm.production.shared.observability.registry

import com.carslab.crm.production.shared.observability.config.MetricsConfiguration
import com.carslab.crm.production.shared.observability.context.MetricsContext
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Component
class CustomMetricsRegistry(
    private val meterRegistry: MeterRegistry,
    private val metricsContext: MetricsContext
) {
    private val logger = LoggerFactory.getLogger(CustomMetricsRegistry::class.java)

    private val timerCache = ConcurrentHashMap<String, Timer>()
    private val counterCache = ConcurrentHashMap<String, Counter>()

    fun recordHttpRequest(
        method: String,
        endpoint: String,
        status: String,
        duration: Duration,
        companyId: String = metricsContext.getCurrentCompanyId()
    ) {
        try {
            val timer = getOrCreateTimer(
                "http_request_duration_seconds",
                mapOf(
                    "method" to method,
                    "endpoint" to sanitizeEndpoint(endpoint),
                    "status" to status,
                    MetricsConfiguration.COMMON_TAG_COMPANY_ID to companyId
                )
            )
            timer.record(duration)

            val counter = getOrCreateCounter(
                "http_requests_total",
                mapOf(
                    "method" to method,
                    "endpoint" to sanitizeEndpoint(endpoint),
                    "status" to status,
                    MetricsConfiguration.COMMON_TAG_COMPANY_ID to companyId
                )
            )
            counter.increment()
        } catch (e: Exception) {
            logger.warn("Failed to record HTTP metrics: ${e.message}")
        }
    }

    fun recordDatabaseQuery(
        repository: String,
        method: String,
        duration: Duration,
        success: Boolean = true,
        errorType: String? = null,
        companyId: String = metricsContext.getCurrentCompanyId()
    ) {
        try {
            val timer = getOrCreateTimer(
                "db_query_duration_seconds",
                mapOf(
                    "repository" to repository,
                    "method" to method,
                    MetricsConfiguration.COMMON_TAG_COMPANY_ID to companyId
                )
            )
            timer.record(duration)

            if (!success && errorType != null) {
                val errorCounter = getOrCreateCounter(
                    "db_query_errors_total",
                    mapOf(
                        "repository" to repository,
                        "method" to method,
                        MetricsConfiguration.COMMON_TAG_ERROR_TYPE to errorType,
                        MetricsConfiguration.COMMON_TAG_COMPANY_ID to companyId
                    )
                )
                errorCounter.increment()
            }
        } catch (e: Exception) {
            logger.warn("Failed to record database metrics: ${e.message}")
        }
    }

    fun recordBusinessOperation(
        service: String,
        operation: String,
        duration: Duration,
        status: String = "success",
        companyId: String = metricsContext.getCurrentCompanyId()
    ) {
        try {
            val timer = getOrCreateTimer(
                "business_operation_duration_seconds",
                mapOf(
                    "service" to service,
                    MetricsConfiguration.COMMON_TAG_OPERATION to operation,
                    MetricsConfiguration.COMMON_TAG_STATUS to status,
                    MetricsConfiguration.COMMON_TAG_COMPANY_ID to companyId
                )
            )
            timer.record(duration)

            val counter = getOrCreateCounter(
                "business_operation_total",
                mapOf(
                    "service" to service,
                    MetricsConfiguration.COMMON_TAG_OPERATION to operation,
                    MetricsConfiguration.COMMON_TAG_STATUS to status,
                    MetricsConfiguration.COMMON_TAG_COMPANY_ID to companyId
                )
            )
            counter.increment()
        } catch (e: Exception) {
            logger.warn("Failed to record business operation metrics: ${e.message}")
        }
    }

    fun recordConnectionPoolMetric(
        activeConnections: Int,
        companyId: String = metricsContext.getCurrentCompanyId()
    ) {
        try {
            meterRegistry.gauge(
                "db_connection_pool_active",
                listOf(
                    io.micrometer.core.instrument.Tag.of(
                        MetricsConfiguration.COMMON_TAG_COMPANY_ID,
                        companyId
                    )
                ),
                activeConnections
            )
        } catch (e: Exception) {
            logger.warn("Failed to record connection pool metrics: ${e.message}")
        }
    }

    fun startTimer(): Timer.Sample {
        return Timer.start(meterRegistry)
    }

    fun incrementCounter(name: String, tags: Map<String, String> = emptyMap()) {
        try {
            val enhancedTags = tags + (MetricsConfiguration.COMMON_TAG_COMPANY_ID to metricsContext.getCurrentCompanyId())
            val counter = getOrCreateCounter(name, enhancedTags)
            counter.increment()
        } catch (e: Exception) {
            logger.warn("Failed to increment counter $name: ${e.message}")
        }
    }

    private fun getOrCreateTimer(name: String, tags: Map<String, String>): Timer {
        val cacheKey = "$name:${tags.hashCode()}"
        return timerCache.getOrPut(cacheKey) {
            Timer.builder(name)
                .tags(tags.map { io.micrometer.core.instrument.Tag.of(it.key, it.value) })
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofMinutes(5))
                .register(meterRegistry)
        }
    }

    private fun getOrCreateCounter(name: String, tags: Map<String, String>): Counter {
        val cacheKey = "$name:${tags.hashCode()}"
        return counterCache.getOrPut(cacheKey) {
            Counter.builder(name)
                .tags(tags.map { io.micrometer.core.instrument.Tag.of(it.key, it.value) })
                .register(meterRegistry)
        }
    }

    private fun sanitizeEndpoint(endpoint: String): String {
        return endpoint
            .replace(Regex("\\d+"), "{id}")
            .replace(Regex("[a-f0-9-]{36}"), "{uuid}")
            .replace(Regex("[a-f0-9]{32}"), "{hash}")
            .take(100)
    }

    fun clearCaches() {
        timerCache.clear()
        counterCache.clear()
        metricsContext.clearCache()
    }
}