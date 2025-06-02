package com.carslab.crm.clients.domain.model

import com.carslab.crm.clients.domain.model.shared.AuditInfo
import com.carslab.crm.domain.model.events.ClientEvent
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Reprezentuje unikalny identyfikator klienta.
 */
data class ClientId(val value: Long) {
    companion object {
        fun generate(): ClientId = ClientId(System.currentTimeMillis())
        fun of(value: Long): ClientId = ClientId(value)
    }
}

/**
 * Dane klienta z informacjami audytowymi.
 */
data class Client(
    val id: ClientId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    val taxId: String? = null,
    val notes: String? = null,
    val audit: AuditInfo = AuditInfo()
) {
    val fullName: String get() = "$firstName $lastName"

    fun hasValidContactInfo(): Boolean = email.isNotBlank() || phone.isNotBlank()

    // Event sourcing preparation
    fun apply(event: ClientEvent): Client {
        return when (event) {
            is ClientEvent.ClientCreated -> this
            is ClientEvent.ClientUpdated -> copy(
                firstName = event.firstName,
                lastName = event.lastName,
                email = event.email,
                phone = event.phone,
                address = event.address,
                company = event.company,
                taxId = event.taxId,
                notes = event.notes,
                audit = audit.updated()
            )
            is ClientEvent.ClientDeleted -> this // Handle in aggregate
        }
    }
}

data class ClientWithStatistics(
    val client: Client,
    val statistics: ClientStatistics
)

data class ClientStatistics(
    val clientId: Long,
    val visitCount: Long = 0,
    val totalRevenue: BigDecimal = BigDecimal.ZERO,
    val vehicleCount: Long = 0,
    val lastVisitDate: LocalDateTime? = null
)

data class ClientSummary(
    val id: ClientId,
    val fullName: String,
    val email: String,
    val phone: String
)