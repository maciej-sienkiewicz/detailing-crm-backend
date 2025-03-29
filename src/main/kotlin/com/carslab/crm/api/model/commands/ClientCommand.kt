package com.carslab.crm.api.model.commands

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Komenda dla tworzenia nowego klienta.
 */
data class CreateClientCommand(
    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("phone")
    val phone: String,

    @JsonProperty("address")
    val address: String? = null,

    @JsonProperty("company")
    val company: String? = null,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("notes")
    val notes: String? = null
)

/**
 * Komenda dla aktualizacji istniejącego klienta.
 */
data class UpdateClientCommand(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("phone")
    val phone: String,

    @JsonProperty("address")
    val address: String? = null,

    @JsonProperty("company")
    val company: String? = null,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("notes")
    val notes: String? = null
)

/**
 * DTO dla podstawowych informacji o kliencie.
 */
data class ClientDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("phone")
    val phone: String,

    @JsonProperty("address")
    val address: String? = null,

    @JsonProperty("company")
    val company: String? = null,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)

/**
 * DTO dla rozszerzonych informacji o kliencie, zawierające statystyki.
 */
data class ClientExpandedDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("phone")
    val phone: String,

    @JsonProperty("address")
    val address: String? = null,

    @JsonProperty("company")
    val company: String? = null,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,

    @JsonProperty("total_visits")
    val totalVisits: Int = 0,

    @JsonProperty("total_transactions")
    val totalTransactions: Int = 0,

    @JsonProperty("abandoned_sales")
    val abandonedSales: Int = 0,

    @JsonProperty("total_revenue")
    val totalRevenue: Double = 0.0,

    @JsonProperty("contact_attempts")
    val contactAttempts: Int = 0,

    @JsonProperty("last_visit_date")
    val lastVisitDate: String? = null,

    @JsonProperty("vehicles")
    val vehicles: List<String> = emptyList()
)

/**
 * DTO dla statystyk klienta.
 */
data class ClientStatisticsDto(
    @JsonProperty("total_visits")
    val totalVisits: Long,

    @JsonProperty("total_revenue")
    val totalRevenue: BigDecimal,

    @JsonProperty("vehicle_no")
    val vehicleNo: Long
)

/**
 * Komenda dla tworzenia nowej próby kontaktu.
 */
data class CreateContactAttemptCommand(
    @JsonProperty("client_id")
    val clientId: String,

    @JsonProperty("date")
    val date: String,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("result")
    val result: String
)

/**
 * Komenda dla aktualizacji istniejącej próby kontaktu.
 */
data class UpdateContactAttemptCommand(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("client_id")
    val clientId: String,

    @JsonProperty("date")
    val date: String,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("result")
    val result: String
)

/**
 * DTO dla próby kontaktu.
 */
data class ContactAttemptDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("client_id")
    val clientId: String,

    @JsonProperty("date")
    val date: String,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("result")
    val result: String,

    @JsonProperty("created_at")
    val createdAt: String,

    @JsonProperty("updated_at")
    val updatedAt: String
)