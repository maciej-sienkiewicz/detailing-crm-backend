// src/main/kotlin/com/carslab/crm/modules/company_settings/api/responses/SignatureResponses.kt
package com.carslab.crm.modules.company_settings.api.responses

import com.carslab.crm.modules.company_settings.domain.model.UserSignature
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class UserSignatureResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("userId")
    val userId: Long,

    @JsonProperty("companyId")
    val companyId: Long,

    @JsonProperty("content")
    val content: String,

    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,

    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(signature: UserSignature): UserSignatureResponse = UserSignatureResponse(
            id = signature.id.value,
            userId = signature.userId,
            companyId = signature.companyId,
            content = signature.content,
            createdAt = signature.audit.createdAt,
            updatedAt = signature.audit.updatedAt
        )
    }
}

data class SignatureValidationResponse(
    @JsonProperty("isValid")
    val isValid: Boolean,

    @JsonProperty("errors")
    val errors: List<String>
)