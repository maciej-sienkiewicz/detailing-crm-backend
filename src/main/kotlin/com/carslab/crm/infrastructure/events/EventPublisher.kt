package com.carslab.crm.infrastructure.events

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class EventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}