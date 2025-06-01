package com.carslab.crm.signature.infrastructure.monitoring

import io.micrometer.core.instrument.*
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@Service
class SignatureMetricsService(private val meterRegistry: MeterRegistry) {

    private val signatureRequestsTotal = Counter.builder("signature.requests.total")
        .description("Total number of signature requests")
        .register(meterRegistry)

    private val signatureRequestsSuccess = Counter.builder("signature.requests.success")
        .description("Successful signature requests")
        .register(meterRegistry)

    private val signatureRequestsFailure = Counter.builder("signature.requests.failure")
        .description("Failed signature requests")
        .register(meterRegistry)

    private val signatureCompletionTime = Timer.builder("signature.completion.time")
        .description("Time to complete signature from request to submission")
        .register(meterRegistry)

    private val activeSignatureSessions = AtomicInteger(0)

    private val activeSessionsGauge = Gauge.builder("signature.sessions.active")
        .description("Number of active signature sessions")
        .register(meterRegistry) { activeSignatureSessions.get().toDouble() }

    private val tabletConnectionsGauge = Gauge.builder("tablets.connections.active")
        .description("Number of active tablet connections")
        .register(meterRegistry) { getActiveTabletConnections().toDouble() }

    fun recordSignatureRequestAttempt(tenantId: UUID) {
        signatureRequestsTotal
            .tag("tenant_id", tenantId.toString())
            .increment()

        activeSignatureSessions.incrementAndGet()
    }

    fun recordSignatureRequestSuccess(tenantId: UUID) {
        signatureRequestsSuccess
            .tag("tenant_id", tenantId.toString())
            .increment()
    }

    fun recordSignatureRequestFailure(tenantId: UUID, reason: String) {
        signatureRequestsFailure
            .tag("tenant_id", tenantId.toString())
            .tag("reason", reason)
            .increment()

        activeSignatureSessions.decrementAndGet()
    }

    fun recordSignatureCompletion(tenantId: UUID, duration: Duration) {
        signatureCompletionTime
            .tag("tenant_id", tenantId.toString())
            .record(duration)

        activeSignatureSessions.decrementAndGet()
    }

    fun recordSignatureSubmissionFailure(sessionId: String, errorType: String?) {
        Counter.builder("signature.submission.failure")
            .tag("error_type", errorType ?: "unknown")
            .register(meterRegistry)
            .increment()
    }

    private fun getActiveTabletConnections(): Int {
        // This would be injected from WebSocketHandler
        return 0 // Placeholder
    }
}