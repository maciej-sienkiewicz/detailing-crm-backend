package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ContactAttemptResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("client_id")
    val clientId: String,

    @JsonProperty("date")
    val date: LocalDateTime,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("result")
    val result: String,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)