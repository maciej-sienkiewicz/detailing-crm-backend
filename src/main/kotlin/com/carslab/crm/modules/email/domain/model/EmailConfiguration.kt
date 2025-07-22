package com.carslab.crm.modules.email.domain.model

import java.time.LocalDateTime

data class EmailConfigurationId(val value: Long) {
    companion object {
        fun of(value: Long): EmailConfigurationId = EmailConfigurationId(value)
    }
}

data class EmailConfiguration(
    val id: EmailConfigurationId?,
    val companyId: Long,
    val senderEmail: String,
    val senderName: String,
    val encryptedPassword: String,
    val smtpHost: String,
    val smtpPort: Int,
    val useSSL: Boolean,
    val isEnabled: Boolean,
    val validationStatus: ValidationStatus,
    val validationMessage: String?,
    val providerHint: String?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun withValidation(status: ValidationStatus, message: String?): EmailConfiguration {
        return copy(
            validationStatus = status,
            validationMessage = message,
            updatedAt = LocalDateTime.now()
        )
    }

    fun withProviderHint(hint: String?): EmailConfiguration {
        return copy(
            providerHint = hint,
            updatedAt = LocalDateTime.now()
        )
    }
}

enum class ValidationStatus {
    NOT_TESTED,
    VALID,
    INVALID_CREDENTIALS,
    INVALID_SETTINGS,
    CONNECTION_ERROR
}

data class CreateEmailConfiguration(
    val companyId: Long,
    val senderEmail: String,
    val senderName: String,
    val plainPassword: String,
    val smtpHost: String,
    val smtpPort: Int,
    val useSSL: Boolean,
    val isEnabled: Boolean,
    val sendTestEmail: Boolean
)