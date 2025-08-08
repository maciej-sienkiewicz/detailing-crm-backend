// src/main/kotlin/com/carslab/crm/modules/company_settings/domain/model/UserSignature.kt
package com.carslab.crm.modules.company_settings.domain.model

import com.carslab.crm.modules.company_settings.domain.model.shared.AuditInfo
import java.util.UUID

data class UserSignatureId(val value: String) {
    companion object {
        fun generate(): UserSignatureId = UserSignatureId(UUID.randomUUID().toString())
        fun of(value: String): UserSignatureId = UserSignatureId(value)
    }
}

data class UserSignature(
    val id: UserSignatureId,
    val userId: Long,
    val companyId: Long,
    val content: String, // Encrypted base64 signature content
    val audit: AuditInfo = AuditInfo()
) {
    fun updateContent(newContent: String): UserSignature {
        return copy(
            content = newContent,
            audit = audit.updated()
        )
    }
}

data class CreateUserSignature(
    val userId: Long,
    val companyId: Long,
    val content: String,
    val audit: AuditInfo = AuditInfo()
)