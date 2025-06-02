package com.carslab.crm.domain.model.events

import com.carslab.crm.clients.domain.model.ClientId
import java.time.LocalDateTime

sealed class ClientEvent {
    abstract val clientId: ClientId
    abstract val timestamp: LocalDateTime

    data class ClientCreated(
        override val clientId: ClientId,
        val firstName: String,
        val lastName: String,
        val email: String,
        val phone: String,
        val address: String?,
        val company: String?,
        val taxId: String?,
        val notes: String?,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : ClientEvent()

    data class ClientUpdated(
        override val clientId: ClientId,
        val firstName: String,
        val lastName: String,
        val email: String,
        val phone: String,
        val address: String?,
        val company: String?,
        val taxId: String?,
        val notes: String?,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : ClientEvent()

    data class ClientDeleted(
        override val clientId: ClientId,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : ClientEvent()
}