package com.carslab.crm.infrastructure.events

import java.time.LocalDateTime
import java.util.*

/**
 * Base interface for all domain events
 */
interface DomainEvent {
    val eventId: String
    val aggregateId: String
    val aggregateType: String
    val eventType: String
    val occurredAt: LocalDateTime
    val companyId: Long
    val userId: String?
    val userName: String?
    val metadata: Map<String, Any?>
}

/**
 * Abstract base class for domain events
 */
abstract class BaseDomainEvent(
    override val aggregateId: String,
    override val aggregateType: String,
    override val eventType: String,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    override val metadata: Map<String, Any?> = emptyMap(),
) : DomainEvent {
    override val eventId: String = UUID.randomUUID().toString()
    override val occurredAt: LocalDateTime = LocalDateTime.now()
}