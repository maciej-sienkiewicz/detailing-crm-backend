package com.carslab.crm.production.shared.observability.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
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
}