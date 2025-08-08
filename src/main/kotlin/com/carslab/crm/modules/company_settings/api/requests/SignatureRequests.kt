// src/main/kotlin/com/carslab/crm/modules/company_settings/api/requests/SignatureRequests.kt
package com.carslab.crm.modules.company_settings.api.requests

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateSignatureRequest(
    @field:NotBlank(message = "Signature content is required")
    @field:Size(max = 1000000, message = "Signature content cannot exceed 1MB")
    @JsonProperty("content")
    val content: String
)

data class UpdateSignatureRequest(
    @field:NotBlank(message = "Signature content is required")
    @field:Size(max = 1000000, message = "Signature content cannot exceed 1MB")
    @JsonProperty("content")
    val content: String
)

data class SignatureValidationRequest(
    @field:NotBlank(message = "Content to validate is required")
    @JsonProperty("content")
    val content: String
)