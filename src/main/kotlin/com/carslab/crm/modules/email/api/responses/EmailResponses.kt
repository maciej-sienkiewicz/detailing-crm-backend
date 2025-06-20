package com.carslab.crm.modules.email.api.responses

import com.carslab.crm.modules.email.application.queries.models.EmailHistoryReadModel
import com.fasterxml.jackson.annotation.JsonProperty

data class EmailSendResponse(
    @JsonProperty("email_id")
    val emailId: String?,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("message")
    val message: String
)

data class EmailHistoryResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("protocol_id")
    val protocolId: String,

    @JsonProperty("recipient_email")
    val recipientEmail: String,

    @JsonProperty("subject")
    val subject: String,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("sent_at")
    val sentAt: String,

    @JsonProperty("error_message")
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