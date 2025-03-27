package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ClientResponse(
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