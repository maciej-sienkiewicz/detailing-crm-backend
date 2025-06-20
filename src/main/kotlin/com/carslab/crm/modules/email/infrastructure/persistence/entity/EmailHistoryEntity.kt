package com.carslab.crm.modules.email.infrastructure.persistence.entity

import com.carslab.crm.modules.email.domain.model.EmailHistory
import com.carslab.crm.modules.email.domain.model.EmailHistoryId
import com.carslab.crm.modules.email.domain.model.EmailStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "email_history",
    indexes = [
        Index(name = "idx_email_history_company_id", columnList = "company_id"),
        Index(name = "idx_email_history_protocol_id", columnList = "protocol_id"),
        Index(name = "idx_email_history_sent_at", columnList = "sent_at")
    ]
)
class EmailHistoryEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "protocol_id", nullable = false)
    val protocolId: String,

    @Column(name = "recipient_email", nullable = false)
    var recipientEmail: String,

    @Column(name = "subject", nullable = false)
    var subject: String,

    @Column(name = "html_content", nullable = false, columnDefinition = "TEXT")
    var htmlContent: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: EmailStatus,

    @Column(name = "sent_at", nullable = false)
    var sentAt: LocalDateTime,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "template_name", nullable = false)
    var templateName: String = "DEFAULT_PROTOCOL_TEMPLATE"
) {
    fun toDomain(): EmailHistory = EmailHistory(
        id = EmailHistoryId(id),
        companyId = companyId,
        protocolId = protocolId,
        recipientEmail = recipientEmail,
        subject = subject,
        htmlContent = htmlContent,
        status = status,
        sentAt = sentAt,
        errorMessage = errorMessage,
        templateName = templateName
    )

    companion object {
        fun fromDomain(domain: EmailHistory): EmailHistoryEntity = EmailHistoryEntity(
            id = domain.id.value,
            companyId = domain.companyId,
            protocolId = domain.protocolId,
            recipientEmail = domain.recipientEmail,
            subject = domain.subject,
            htmlContent = domain.htmlContent,
            status = domain.status,
            sentAt = domain.sentAt,
            errorMessage = domain.errorMessage,
            templateName = domain.templateName
        )
    }
}