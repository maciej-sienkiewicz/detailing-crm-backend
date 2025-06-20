package com.carslab.crm.modules.email.domain.ports

import com.carslab.crm.domain.model.EmailAttachment

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