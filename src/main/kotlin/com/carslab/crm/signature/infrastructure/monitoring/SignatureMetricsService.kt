package com.carslab.crm.signature.service

import io.micrometer.core.instrument.*
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@Service
class SignatureMetricsService(private val meterRegistry: MeterRegistry) {

    private val activeSignatureSessions = AtomicInteger(0)

    fun recordSignatureRequestAttempt(tenantId: UUID) {
        Counter.builder("signature.requests.total")
            .description("Total number of signature requests")
            .tag("tenant_id", tenantId.toString())
            .register(meterRegistry)
            .increment()

        activeSignatureSessions.incrementAndGet()
    }

    fun recordSignatureRequestSuccess(tenantId: UUID) {
        Counter.builder("signature.requests.success")
            .description("Successful signature requests")
            .tag("tenant_id", tenantId.toString())
            .register(meterRegistry)
            .increment()
    }

    fun recordSignatureRequestFailure(tenantId: UUID, reason: String) {
        Counter.builder("signature.requests.failure")
            .description("Failed signature requests")
            .tag("tenant_id", tenantId.toString())
            .tag("reason", reason)
            .register(meterRegistry)
            .increment()

        activeSignatureSessions.decrementAndGet()
    }

    fun recordSignatureCompletion(tenantId: UUID, duration: Duration) {
        Timer.builder("signature.completion.time")
            .description("Time to complete signature from request to submission")
            .tag("tenant_id", tenantId.toString())
            .register(meterRegistry)
            .record(duration)

        activeSignatureSessions.decrementAndGet()
    }

    fun recordSignatureSubmissionFailure(sessionId: String, errorType: String?) {
        Counter.builder("signature.submission.failure")
            .description("Failed signature submissions")
            .tag("error_type", errorType ?: "unknown")
            .register(meterRegistry)
            .increment()
    }

    // Helper method to get current active sessions count
    fun getActiveSessionsCount(): Int = activeSignatureSessions.get()
}