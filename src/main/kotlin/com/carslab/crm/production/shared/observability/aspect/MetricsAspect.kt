package com.carslab.crm.production.shared.observability.aspect

import com.carslab.crm.production.shared.observability.annotations.DatabaseMonitored
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.observability.annotations.TimedOperation
import com.carslab.crm.production.shared.observability.registry.CustomMetricsRegistry
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Duration
import java.time.Instant

@Aspect
@Component
@Order(1)
class MetricsAspect(
    private val metricsRegistry: CustomMetricsRegistry
) {
    private val logger = LoggerFactory.getLogger(MetricsAspect::class.java)

    @Around("@annotation(timedOperation)")
    fun measureTimedOperation(joinPoint: ProceedingJoinPoint, timedOperation: TimedOperation): Any? {
        val startTime = Instant.now()
        var status = "success"
        var exception: Throwable? = null

        return try {
            val result = joinPoint.proceed()
            result
        } catch (ex: Throwable) {
            exception = ex
            status = "error"
            throw ex
        } finally {
            try {
                val duration = Duration.between(startTime, Instant.now())
                val shouldRecord = (status == "success" && timedOperation.recordSuccesses) ||
                        (status == "error" && timedOperation.recordFailures)

                if (shouldRecord) {
                    metricsRegistry.recordBusinessOperation(
                        service = timedOperation.module,
                        operation = timedOperation.operation,
                        duration = duration,
                        status = status
                    )
                }

                if (exception != null) {
                    metricsRegistry.incrementCounter(
                        "business_operation_errors_total",
                        mapOf(
                            "service" to timedOperation.module,
                            "operation" to timedOperation.operation,
                            "error_type" to getErrorType(exception)
                        )
                    )
                }
            } catch (e: Exception) {
                logger.warn("Failed to record metrics for ${timedOperation.operation}: ${e.message}")
            }
        }
    }

    @Around("@annotation(databaseMonitored)")
    fun measureDatabaseOperation(joinPoint: ProceedingJoinPoint, databaseMonitored: DatabaseMonitored): Any? {
        val startTime = Instant.now()
        var success = true
        var exception: Throwable? = null

        return try {
            val result = joinPoint.proceed()
            result
        } catch (ex: Throwable) {
            exception = ex
            success = false
            throw ex
        } finally {
            try {
                val duration = Duration.between(startTime, Instant.now())

                if (!databaseMonitored.recordOnlyOnError || !success) {
                    val errorType = if (exception != null) getErrorType(exception) else null

                    metricsRegistry.recordDatabaseQuery(
                        repository = databaseMonitored.repository,
                        method = databaseMonitored.method,
                        operation = databaseMonitored.operation,
                        duration = duration,
                        success = success,
                        errorType = errorType
                    )
                }

                if (duration.toMillis() > databaseMonitored.timeoutMs) {
                    metricsRegistry.incrementCounter(
                        "db_slow_queries_total",
                        mapOf(
                            "repository" to databaseMonitored.repository,
                            "method" to databaseMonitored.method
                        )
                    )
                }
            } catch (e: Exception) {
                logger.warn("Failed to record database metrics for ${databaseMonitored.method}: ${e.message}")
            }
        }
    }

    @Around("@annotation(httpMonitored)")
    fun measureHttpOperation(joinPoint: ProceedingJoinPoint, httpMonitored: HttpMonitored): Any? {
        val startTime = Instant.now()
        var statusCode = "200"
        var method = "UNKNOWN"
        var endpoint = httpMonitored.endpoint

        return try {
            val request = getCurrentHttpRequest()
            method = request?.method ?: "UNKNOWN"

            if (endpoint.isEmpty()) {
                endpoint = request?.requestURI ?: "unknown"
            }

            val result = joinPoint.proceed()
            result
        } catch (ex: Throwable) {
            statusCode = when (ex) {
                is IllegalArgumentException -> "400"
                is SecurityException -> "403"
                is NoSuchElementException -> "404"
                is IllegalStateException -> "409"
                else -> "500"
            }
            throw ex
        } finally {
            try {
                if (httpMonitored.recordResponseTime) {
                    val duration = Duration.between(startTime, Instant.now())

                    metricsRegistry.recordHttpRequest(
                        method = method,
                        endpoint = endpoint,
                        status = statusCode,
                        duration = duration
                    )
                }

                if (httpMonitored.recordPayloadSize) {
                    val request = getCurrentHttpRequest()
                    val contentLength = request?.contentLength ?: 0
                    if (contentLength > 0) {
                        metricsRegistry.incrementCounter(
                            "http_request_payload_bytes_total",
                            mapOf(
                                "method" to method,
                                "endpoint" to endpoint
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to record HTTP metrics for $endpoint: ${e.message}")
            }
        }
    }

    private fun getCurrentHttpRequest(): HttpServletRequest? {
        return try {
            val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            requestAttributes?.request
        } catch (e: Exception) {
            null
        }
    }

    private fun getErrorType(exception: Throwable): String {
        return when (exception) {
            is IllegalArgumentException -> "validation_error"
            is IllegalStateException -> "state_error"
            is SecurityException -> "security_error"
            is NoSuchElementException -> "not_found"
            is RuntimeException -> "runtime_error"
            is org.springframework.dao.DataAccessException -> "database_error"
            is org.springframework.transaction.TransactionException -> "transaction_error"
            is java.util.concurrent.TimeoutException -> "timeout_error"
            else -> exception.javaClass.simpleName.lowercase()
                .replace("exception", "")
                .replace("error", "")
                .takeIf { it.isNotBlank() } ?: "unknown"
        }
    }
}