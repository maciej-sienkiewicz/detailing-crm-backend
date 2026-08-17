package com.carslab.crm.modules.communication.infrastructure.persistence.entity

import com.carslab.crm.modules.communication.domain.model.LeadFactKey
import com.carslab.crm.modules.communication.domain.model.LeadStage
import com.carslab.crm.modules.communication.domain.model.MailAccountStatus
import com.carslab.crm.modules.communication.domain.model.MailAuthType
import com.carslab.crm.modules.communication.domain.model.MailProviderType
import com.carslab.crm.modules.communication.domain.model.MessageDirection
import com.carslab.crm.modules.communication.domain.model.MessageSendStatus
import com.carslab.crm.modules.communication.domain.model.ThreadClassification
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "comm_mail_account",
    indexes = [Index(name = "idx_comm_account_company", columnList = "company_id")],
    uniqueConstraints = [UniqueConstraint(name = "uq_comm_account_email", columnNames = ["company_id", "email_address"])]
)
class MailAccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "email_address", nullable = false, length = 255)
    val emailAddress: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 30)
    var providerType: MailProviderType,

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 30)
    var authType: MailAuthType,

    @Column(name = "encrypted_password", length = 1000)
    var encryptedPassword: String? = null,

    @Column(name = "imap_host", length = 255)
    var imapHost: String? = null,

    @Column(name = "imap_port")
    var imapPort: Int? = null,

    @Column(name = "smtp_host", length = 255)
    var smtpHost: String? = null,

    @Column(name = "smtp_port")
    var smtpPort: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: MailAccountStatus = MailAccountStatus.ACTIVE,

    @Column(name = "last_error", length = 500)
    var lastError: String? = null,

    @Column(name = "last_sync_at")
    var lastSyncAt: LocalDateTime? = null,

    @Column(name = "inbox_uid_validity")
    var inboxUidValidity: Long? = null,

    @Column(name = "inbox_last_uid")
    var inboxLastUid: Long? = null,

    @Column(name = "sent_uid_validity")
    var sentUidValidity: Long? = null,

    @Column(name = "sent_last_uid")
    var sentLastUid: Long? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "comm_mail_thread",
    indexes = [
        Index(name = "idx_comm_thread_company", columnList = "company_id"),
        Index(name = "idx_comm_thread_last_msg", columnList = "company_id,last_message_at"),
        Index(name = "idx_comm_thread_ext_key", columnList = "company_id,external_thread_key")
    ]
)
class MailThreadEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "mail_account_id", nullable = false)
    val mailAccountId: Long,

    @Column(name = "subject_norm", length = 500)
    var subjectNorm: String? = null,

    @Column(name = "external_thread_key", length = 255)
    var externalThreadKey: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 20)
    var classification: ThreadClassification = ThreadClassification.PENDING,

    @Column(name = "last_message_at", nullable = false)
    var lastMessageAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "last_direction", length = 10)
    var lastDirection: MessageDirection? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "comm_mail_message",
    indexes = [
        Index(name = "idx_comm_msg_thread", columnList = "thread_id,sent_at"),
        Index(name = "idx_comm_msg_company_msgid", columnList = "company_id,message_id")
    ],
    uniqueConstraints = [UniqueConstraint(name = "uq_comm_msg_account_msgid", columnNames = ["mail_account_id", "message_id"])]
)
class MailMessageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "thread_id", nullable = false)
    val threadId: Long,

    @Column(name = "mail_account_id", nullable = false)
    val mailAccountId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    val direction: MessageDirection,

    /** RFC 5322 Message-ID, normalized (no angle brackets). */
    @Column(name = "message_id", nullable = false, length = 500)
    val messageId: String,

    @Column(name = "in_reply_to", length = 500)
    val inReplyTo: String? = null,

    /** Space-separated Message-IDs from the References header, oldest first. */
    @Column(name = "references_ids", columnDefinition = "TEXT")
    val referencesIds: String? = null,

    @Column(name = "from_email", nullable = false, length = 320)
    val fromEmail: String,

    @Column(name = "from_name", length = 255)
    val fromName: String? = null,

    /** Comma-separated recipient addresses. */
    @Column(name = "to_emails", columnDefinition = "TEXT")
    val toEmails: String? = null,

    @Column(name = "subject", length = 1000)
    val subject: String? = null,

    @Column(name = "sent_at", nullable = false)
    val sentAt: LocalDateTime,

    @Column(name = "body_html", columnDefinition = "TEXT")
    val bodyHtml: String? = null,

    @Column(name = "body_text_clean", columnDefinition = "TEXT")
    var bodyTextClean: String? = null,

    @Column(name = "has_attachments", nullable = false)
    val hasAttachments: Boolean = false,

    @Column(name = "provider_uid", length = 255)
    var providerUid: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false, length = 20)
    var sendStatus: MessageSendStatus = MessageSendStatus.RECEIVED,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "comm_lead",
    indexes = [
        Index(name = "idx_comm_lead_company_stage", columnList = "company_id,stage"),
        Index(name = "idx_comm_lead_contact", columnList = "company_id,contact_email")
    ],
    uniqueConstraints = [UniqueConstraint(name = "uq_comm_lead_thread", columnNames = ["thread_id"])]
)
class CommLeadEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "thread_id", nullable = false)
    val threadId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    var stage: LeadStage = LeadStage.NEW,

    @Column(name = "contact_email", nullable = false, length = 320)
    val contactEmail: String,

    @Column(name = "customer_name", length = 255)
    var customerName: String? = null,

    @Column(name = "phone", length = 50)
    var phone: String? = null,

    @Column(name = "vehicle_brand", length = 100)
    var vehicleBrand: String? = null,

    @Column(name = "vehicle_model", length = 100)
    var vehicleModel: String? = null,

    @Column(name = "vehicle_year")
    var vehicleYear: Int? = null,

    /** Last quote total in grosze. */
    @Column(name = "quote_total")
    var quoteTotal: Long? = null,

    @Column(name = "quote_sent_at")
    var quoteSentAt: LocalDateTime? = null,

    @Column(name = "lost_reason", length = 255)
    var lostReason: String? = null,

    @Column(name = "unread", nullable = false)
    var unread: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "comm_lead_fact",
    indexes = [Index(name = "idx_comm_fact_lead", columnList = "lead_id")]
)
class LeadFactEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "lead_id", nullable = false)
    val leadId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "fact_key", nullable = false, length = 30)
    val factKey: LeadFactKey,

    @Column(name = "fact_value", nullable = false, length = 500)
    var value: String,

    @Column(name = "verified", nullable = false)
    var verified: Boolean = false,

    @Column(name = "source_message_id")
    val sourceMessageId: Long? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "comm_lead_quote",
    indexes = [Index(name = "idx_comm_quote_lead", columnList = "lead_id")]
)
class LeadQuoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "lead_id", nullable = false)
    val leadId: Long,

    @Column(name = "message_id")
    val messageId: Long? = null,

    /** JSON array: [{"name": "...", "price": grosze}]. */
    @Column(name = "items_json", nullable = false, columnDefinition = "TEXT")
    val itemsJson: String,

    @Column(name = "total", nullable = false)
    val total: Long,

    @Column(name = "sent_at", nullable = false)
    val sentAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "comm_lead_note",
    indexes = [Index(name = "idx_comm_note_lead", columnList = "lead_id")]
)
class LeadNoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "lead_id", nullable = false)
    val leadId: Long,

    @Column(name = "note_text", nullable = false, columnDefinition = "TEXT")
    val text: String,

    @Column(name = "author", length = 255)
    val author: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "comm_lead_event",
    indexes = [Index(name = "idx_comm_event_lead", columnList = "lead_id")]
)
class LeadEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "lead_id", nullable = false)
    val leadId: Long,

    @Column(name = "event_text", nullable = false, length = 500)
    val text: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "comm_sender_rule",
    uniqueConstraints = [UniqueConstraint(name = "uq_comm_sender_rule", columnNames = ["company_id", "pattern"])]
)
class SenderRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    /** Full email address or bare domain the rule applies to. */
    @Column(name = "pattern", nullable = false, length = 320)
    val pattern: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 20)
    val classification: ThreadClassification,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
