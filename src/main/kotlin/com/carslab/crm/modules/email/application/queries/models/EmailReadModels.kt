package com.carslab.crm.modules.email.application.queries.models

data class EmailHistoryReadModel(
    val id: String,
    val protocolId: String,
    val recipientEmail: String,
    val subject: String,
    val status: String,
    val sentAt: String,
    val errorMessage: String?
)

data class EmailHistoryResponse(
    val id: String,
    val protocolId: String,
    val recipientEmail: String,
    val subject: String,
    val status: String,
    val sentAt: String,
    val errorMessage: String?
) {
    companion object {
        fun from(history: EmailHistoryReadModel): EmailHistoryResponse = EmailHistoryResponse(
            id = history.id,
            protocolId = history.protocolId,
            recipientEmail = history.recipientEmail,
            subject = history.subject,
            status = history.status,
            sentAt = history.sentAt,
            errorMessage = history.errorMessage
        )
    }
}