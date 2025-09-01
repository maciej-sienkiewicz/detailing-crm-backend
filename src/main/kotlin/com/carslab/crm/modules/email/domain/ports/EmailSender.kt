package com.carslab.crm.modules.email.domain.ports

interface EmailSender {
    fun sendEmail(
        recipientEmail: String,
        subject: String,
        htmlContent: String,
        senderName: String?,
        senderEmail: String?,
        attachment: EmailAttachment?
    ): Boolean
}

data class EmailAttachment(
    val filename: String,
    val content: ByteArray,
    val contentType: String
)