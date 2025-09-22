package com.carslab.crm.modules.email.domain.ports

import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext

interface EmailSender {
    fun sendEmail(
        recipientEmail: String,
        subject: String,
        htmlContent: String,
        senderName: String?,
        senderEmail: String?,
        attachment: EmailAttachment?,
        authContext: AuthContext? = null
    ): Boolean
}

data class EmailAttachment(
    val filename: String,
    val content: ByteArray,
    val contentType: String
)