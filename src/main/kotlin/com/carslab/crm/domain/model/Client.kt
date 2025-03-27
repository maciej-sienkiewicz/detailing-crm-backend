package com.carslab.crm.domain.model

import com.carslab.crm.domain.model.stats.ClientStats
import java.time.LocalDateTime
import java.util.UUID

/**
 * Reprezentuje unikalny identyfikator klienta.
 */
data class ClientId(val value: Long = System.currentTimeMillis())

/**
 * Dane klienta z informacjami audytowymi.
 */
data class ClientDetails(
    val id: ClientId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    val taxId: String? = null,
    val notes: String? = null,
    val audit: ClientAudit = ClientAudit()
) {
    /**
     * Sprawdza, czy klient ma prawidłowe dane kontaktowe.
     */
    fun hasValidContactInfo(): Boolean {
        return email.isNotBlank() || phone.isNotBlank()
    }

    /**
     * Pełne imię i nazwisko klienta.
     */
    val fullName: String
        get() = "$firstName $lastName"
}

data class ClientStats(
    val client: ClientDetails,
    val vehicles: List<Vehicle>,
    val stats: ClientStats? = null
)

/**
 * Informacje audytowe dla klienta.
 */
data class ClientAudit(
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)