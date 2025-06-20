package com.carslab.crm.modules.email.api.requests

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SendProtocolEmailRequest(
    @field:NotBlank(message = "Protocol ID is required")
    @JsonProperty("protocol_id")
    val protocolId: String,

    @field:Email(message = "Valid recipient email is required")
    @JsonProperty("recipient_email")
    val recipientEmail: String? = null,

    @JsonProperty("custom_subject")
    val customSubject: String? = null,

    @JsonProperty("additional_variables")
    val additionalVariables: Map<String, String> = emptyMap()
)