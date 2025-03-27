package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ClientExpandedResponse(
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

    // Nowe pola statystyczne
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