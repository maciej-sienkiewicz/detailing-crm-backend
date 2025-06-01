package com.carslab.crm.signature.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class SignatureMetricsService(
    private val meterRegistry: MeterRegistry
) {

    // Counters
    private val signatureRequestsSentCounter = Counter.builder("signature.requests.sent")
        .description("Number of signature requests sent to tablets")
        .register(meterRegistry)

    private val signatureRequestsFailedCounter = Counter.builder("signature.requests.failed")
        .description("Number of failed signature requests")
        .register(meterRegistry)

    private val signaturesCompletedCounter = Counter.builder("signatures.completed")
        .description("Number of completed signatures")
        .register(meterRegistry)

    private val signaturesExpiredCounter = Counter.builder("signatures.expired")
        .description("Number of expired signatures")
        .register(meterRegistry)

    private val signatureSubmissionsFailedCounter = Counter.builder("signature.submissions.failed")
        .description("Number of failed signature submissions")
        .register(meterRegistry)

    // Timers
    private val sessionCreationTimer = Timer.builder("signature.session.creation.time")
        .description("Time taken to create signature session")
        .register(meterRegistry)

    private val signatureProcessingTimer = Timer.builder("signature.processing.time")
        .description("Time taken to process signature submission")
        .register(meterRegistry)

    // Increment methods
    fun incrementSignatureRequestsSent() {
        signatureRequestsSentCounter.increment()
    }

    fun incrementSignatureRequestsFailed() {
        signatureRequestsFailedCounter.increment()
    }

    fun incrementSignaturesCompleted() {
        signaturesCompletedCounter.increment()
    }

    fun incrementSignaturesExpired() {
        signaturesExpiredCounter.increment()
    }

    fun incrementSignatureSubmissionsFailed() {
        signatureSubmissionsFailedCounter.increment()
    }

    // Timer methods
    fun recordSessionCreationTime(startTime: Instant) {
        val duration = Duration.between(startTime, Instant.now())
        sessionCreationTimer.record(duration)
    }

    fun recordSignatureProcessingTime(startTime: Instant) {
        val duration = Duration.between(startTime, Instant.now())
        signatureProcessingTimer.record(duration)
    }

    // Custom metrics
    fun recordSignatureRequestLatency(latency: Duration) {
        Timer.builder("signature.request.latency")
            .description("Latency of signature requests")
            .register(meterRegistry)
            .record(latency)
    }

    fun recordTabletResponseTime(tabletId: java.util.UUID, responseTime: Duration) {
        Timer.builder("tablet.response.time")
            .description("Response time from tablets")
            .tag("tablet_id", tabletId.toString())
            .register(meterRegistry)
            .record(responseTime)
    }
}