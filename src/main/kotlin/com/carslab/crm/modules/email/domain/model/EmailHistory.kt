package com.carslab.crm.modules.email.domain.model

import java.time.LocalDateTime

data class EmailHistoryId(val value: String) {
    companion object {
        fun generate(): EmailHistoryId = EmailHistoryId(java.util.UUID.randomUUID().toString())
    }
}

data class EmailHistory(
    val id: EmailHistoryId,
    val companyId: Long,
    val protocolId: String,
    val recipientEmail: String,
    val subject: String,
    val htmlContent: String,
    val status: EmailStatus,
    val sentAt: LocalDateTime,
    val errorMessage: String? = null,
    val templateName: String = "DEFAULT_PROTOCOL_TEMPLATE"
)

enum class EmailStatus {
    PENDING,
    SENT,
    FAILED,
    BOUNCED
}