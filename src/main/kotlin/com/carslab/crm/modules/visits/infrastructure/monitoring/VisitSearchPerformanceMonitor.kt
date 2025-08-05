package com.carslab.crm.modules.visits.infrastructure.monitoring

import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

@Component
class VisitSearchPerformanceMonitor {

    private val logger = LoggerFactory.getLogger(VisitSearchPerformanceMonitor::class.java)

    fun <T> monitorQuery(queryName: String, parameters: Map<String, Any?>, operation: () -> T): T {
        val startTime = Instant.now()

        return try {
            val result = operation()
            val duration = Duration.between(startTime, Instant.now())

            if (duration.toMillis() > 1000) {
                logger.warn("Slow query detected: {} took {}ms with parameters: {}",
                    queryName, duration.toMillis(), parameters)
            } else {
                logger.debug("Query {} completed in {}ms", queryName, duration.toMillis())
            }

            result
        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())
            logger.error("Query {} failed after {}ms with parameters: {}",
                queryName, duration.toMillis(), parameters, e)
            throw e
        }
    }
}
