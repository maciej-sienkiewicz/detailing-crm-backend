package com.carslab.crm.modules.email.api.requests

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class SendProtocolEmailRequest(
    @field:NotBlank(message = "Visit ID is required")
    @JsonProperty("visit_id")
    val visit_id: String,
)