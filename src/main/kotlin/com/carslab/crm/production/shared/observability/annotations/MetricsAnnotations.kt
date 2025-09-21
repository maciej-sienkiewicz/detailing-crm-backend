package com.carslab.crm.production.shared.observability.annotations

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TimedOperation(
    val operation: String,
    val module: String,
    val recordFailures: Boolean = true,
    val recordSuccesses: Boolean = true,
    val percentiles: DoubleArray = [],
    val histogram: Boolean = false
)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DatabaseMonitored(
    val repository: String,
    val method: String,
    val recordOnlyOnError: Boolean = false,
    val timeoutMs: Long = 30000,
    val operation: String,
)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HttpMonitored(
    val endpoint: String,
    val includeClientInfo: Boolean = false,
    val recordResponseTime: Boolean = true,
    val recordPayloadSize: Boolean = false
)