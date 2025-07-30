package com.carslab.crm.signature.events

import com.carslab.crm.infrastructure.events.DomainEvent
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class DocumentSignatureCompletedEvent(
    val sessionId: String,
    val signatureImage: String,
    val tabletId: UUID,
    val signatureCompanyId: Long,
    override val eventId: String = UUID.randomUUID().toString(),
    override val aggregateId: String = sessionId,
    override val aggregateType: String = "DocumentSignatureSession",
    override val eventType: String = "DocumentSignatureCompleted",
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val companyId: Long = signatureCompanyId,
    override val userId: String? = null,
    override val userName: String? = null,
    override val metadata: Map<String, Any?> = mapOf(
        "tabletId" to tabletId.toString(),
        "sessionId" to sessionId,
        "hasSignature" to true
    )
) : DomainEvent {
    constructor(
        sessionId: String,
        signatureImage: String,
        tabletId: UUID,
        companyId: Long
    ) : this(
        sessionId = sessionId,
        signatureImage = signatureImage,
        tabletId = tabletId,
        signatureCompanyId = companyId
    )
}