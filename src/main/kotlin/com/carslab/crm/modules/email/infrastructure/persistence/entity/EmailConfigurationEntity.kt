package com.carslab.crm.modules.email.infrastructure.persistence.entity

import com.carslab.crm.modules.email.domain.model.EmailConfiguration
import com.carslab.crm.modules.email.domain.model.EmailConfigurationId
import com.carslab.crm.modules.email.domain.model.ValidationStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "email_configuration",
    indexes = [
        Index(name = "idx_email_config_company_id", columnList = "company_id", unique = true)
    ]
)
class EmailConfigurationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false, unique = true)
    val companyId: Long,

    @Column(name = "sender_email", nullable = false, length = 255)
    var senderEmail: String,

    @Column(name = "sender_name", nullable = false, length = 255)
    var senderName: String,

    @Column(name = "encrypted_password", nullable = false, length = 500)
    var encryptedPassword: String,

    @Column(name = "smtp_host", nullable = false, length = 255)
    var smtpHost: String,

    @Column(name = "smtp_port", nullable = false)
    var smtpPort: Int,

    @Column(name = "use_ssl", nullable = false)
    var useSSL: Boolean,

    @Column(name = "is_enabled", nullable = false)
    var isEnabled: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false)
    var validationStatus: ValidationStatus,

    @Column(name = "validation_message", length = 500)
    var validationMessage: String? = null,

    @Column(name = "provider_hint", length = 255)
    var providerHint: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): EmailConfiguration = EmailConfiguration(
        id = id?.let { EmailConfigurationId.of(it) },
        companyId = companyId,
        senderEmail = senderEmail,
        senderName = senderName,
        encryptedPassword = encryptedPassword,
        smtpHost = smtpHost,
        smtpPort = smtpPort,
        useSSL = useSSL,
        isEnabled = isEnabled,
        validationStatus = validationStatus,
        validationMessage = validationMessage,
        providerHint = providerHint,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(configuration: EmailConfiguration): EmailConfigurationEntity = EmailConfigurationEntity(
            id = configuration.id?.value,
            companyId = configuration.companyId,
            senderEmail = configuration.senderEmail,
            senderName = configuration.senderName,
            encryptedPassword = configuration.encryptedPassword,
            smtpHost = configuration.smtpHost,
            smtpPort = configuration.smtpPort,
            useSSL = configuration.useSSL,
            isEnabled = configuration.isEnabled,
            validationStatus = configuration.validationStatus,
            validationMessage = configuration.validationMessage,
            providerHint = configuration.providerHint,
            createdAt = configuration.createdAt,
            updatedAt = configuration.updatedAt
        )
    }
}