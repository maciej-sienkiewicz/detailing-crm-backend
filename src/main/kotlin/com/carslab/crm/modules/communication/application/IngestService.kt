package com.carslab.crm.modules.communication.application

import com.carslab.crm.modules.communication.domain.model.LeadFactKey
import com.carslab.crm.modules.communication.domain.model.MessageDirection
import com.carslab.crm.modules.communication.domain.model.MessageSendStatus
import com.carslab.crm.modules.communication.domain.model.ThreadClassification
import com.carslab.crm.modules.communication.domain.ports.InquiryClassificationInput
import com.carslab.crm.modules.communication.domain.ports.InquiryClassifier
import com.carslab.crm.modules.communication.domain.ports.InquiryExtractor
import com.carslab.crm.modules.communication.domain.services.EmailCleaningService
import com.carslab.crm.modules.communication.domain.services.ThreadingService
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.CommLeadEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.LeadFactEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailAccountEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailMessageEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.CommLeadJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.LeadFactJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.MailMessageJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.SenderRuleJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** Normalized incoming e-mail, independent of the source adapter (IMAP, Gmail API, Graph, legacy webhook). */
data class RawIncomingEmail(
    val messageId: String?,
    val inReplyTo: String?,
    val references: List<String>,
    val externalThreadKey: String? = null,
    val fromEmail: String,
    val fromName: String?,
    val toEmails: List<String>,
    val subject: String?,
    val sentAt: LocalDateTime,
    val bodyHtml: String?,
    val bodyText: String?,
    val hasAttachments: Boolean = false,
    val providerUid: String? = null,
    val direction: MessageDirection,
    /** Selected raw headers used by the F0 rules (lowercase keys). */
    val headers: Map<String, String> = emptyMap()
)

/**
 * Single entry point for every mail source. Pipeline per architecture doc:
 * dedupe -> threading -> denoise -> F0 zero-cost rules -> F1 thread class
 * inheritance -> F2 classification -> F3 extraction -> lead upsert.
 */
@Service
class IngestService(
    private val threadingService: ThreadingService,
    private val cleaningService: EmailCleaningService,
    private val classifier: InquiryClassifier,
    private val extractor: InquiryExtractor,
    private val messageRepository: MailMessageJpaRepository,
    private val leadRepository: CommLeadJpaRepository,
    private val factRepository: LeadFactJpaRepository,
    private val senderRuleRepository: SenderRuleJpaRepository
) {
    private val logger = LoggerFactory.getLogger(IngestService::class.java)

    @Transactional
    fun ingest(account: MailAccountEntity, email: RawIncomingEmail) {
        val accountId = requireNotNull(account.id)
        val messageId = ThreadingService.normalizeMessageId(email.messageId)
            ?: ThreadingService.syntheticMessageId(email.fromEmail, email.sentAt, email.subject)

        val existing = messageRepository.findByMailAccountIdAndMessageId(accountId, messageId)
        if (existing != null) {
            // Sent-folder copy of a message the CRM itself sent: enrich, don't duplicate.
            if (existing.providerUid == null && email.providerUid != null) {
                existing.providerUid = email.providerUid
                messageRepository.save(existing)
            }
            return
        }

        val counterparty = if (email.direction == MessageDirection.INBOUND) {
            email.fromEmail
        } else {
            email.toEmails.firstOrNull() ?: email.fromEmail
        }

        val thread = threadingService.resolveThread(
            companyId = account.companyId,
            mailAccountId = accountId,
            messageIdHeader = messageId,
            inReplyTo = ThreadingService.normalizeMessageId(email.inReplyTo),
            references = email.references,
            externalThreadKey = email.externalThreadKey,
            counterpartyEmail = counterparty,
            subject = email.subject,
            sentAt = email.sentAt
        )
        val threadId = requireNotNull(thread.id)

        val bodyClean = cleaningService.clean(email.bodyHtml, email.bodyText)

        val message = messageRepository.save(
            MailMessageEntity(
                companyId = account.companyId,
                threadId = threadId,
                mailAccountId = accountId,
                direction = email.direction,
                messageId = messageId,
                inReplyTo = ThreadingService.normalizeMessageId(email.inReplyTo),
                referencesIds = email.references.joinToString(" ").ifBlank { null },
                fromEmail = email.fromEmail.lowercase(),
                fromName = email.fromName?.take(255),
                toEmails = email.toEmails.joinToString(",").ifBlank { null },
                subject = email.subject?.take(1000),
                sentAt = email.sentAt,
                bodyHtml = email.bodyHtml,
                bodyTextClean = bodyClean,
                hasAttachments = email.hasAttachments,
                providerUid = email.providerUid,
                sendStatus = if (email.direction == MessageDirection.OUTBOUND) MessageSendStatus.SENT else MessageSendStatus.RECEIVED
            )
        )

        threadingService.touchThread(thread, email.sentAt, email.direction)

        val classification = classifyThread(account, email, bodyClean, thread.classification)
        if (thread.classification != classification) {
            thread.classification = classification
        }

        if (classification == ThreadClassification.INQUIRY) {
            upsertLead(account, threadId, email, bodyClean, message)
        } else {
            // A reply landing in an already-tracked inquiry thread still updates the lead.
            leadRepository.findByThreadId(threadId)?.let { lead ->
                if (email.direction == MessageDirection.INBOUND) lead.unread = true
                lead.updatedAt = LocalDateTime.now()
                leadRepository.save(lead)
            }
        }
    }

    private fun classifyThread(
        account: MailAccountEntity,
        email: RawIncomingEmail,
        bodyClean: String,
        current: ThreadClassification
    ): ThreadClassification {
        // F1: a classified thread inherits its class — every follow-up message is free.
        if (current != ThreadClassification.PENDING) return current

        // Outbound-first threads (owner wrote first) are not inquiries until the client replies.
        if (email.direction == MessageDirection.OUTBOUND) return ThreadClassification.PENDING

        // F0: zero-cost rules.
        if (email.headers.containsKey("list-unsubscribe") || email.headers.containsKey("list-id")) {
            return ThreadClassification.OTHER
        }
        val autoSubmitted = email.headers["auto-submitted"]
        if (autoSubmitted != null && !autoSubmitted.equals("no", ignoreCase = true)) {
            return ThreadClassification.OTHER
        }
        val domain = email.fromEmail.substringAfterLast('@').lowercase()
        val rules = senderRuleRepository.findByCompanyIdAndPatternIn(
            account.companyId, listOf(email.fromEmail.lowercase(), domain)
        )
        rules.firstOrNull()?.let { return it.classification }

        // F2: classifier (heuristic fallback or LLM-backed bean).
        return try {
            classifier.classify(
                InquiryClassificationInput(
                    fromEmail = email.fromEmail,
                    subject = email.subject,
                    bodySnippet = bodyClean.take(1500)
                )
            )
        } catch (e: Exception) {
            logger.warn("Classification failed for account ${account.id}, leaving thread UNSURE", e)
            ThreadClassification.UNSURE
        }
    }

    private fun upsertLead(
        account: MailAccountEntity,
        threadId: Long,
        email: RawIncomingEmail,
        bodyClean: String,
        message: MailMessageEntity
    ) {
        val contactEmail = if (email.direction == MessageDirection.INBOUND) {
            email.fromEmail.lowercase()
        } else {
            (email.toEmails.firstOrNull() ?: email.fromEmail).lowercase()
        }

        val lead = leadRepository.findByThreadId(threadId) ?: leadRepository.save(
            CommLeadEntity(
                companyId = account.companyId,
                threadId = threadId,
                contactEmail = contactEmail,
                customerName = email.fromName?.take(255)
            )
        )
        val leadId = requireNotNull(lead.id)

        if (email.direction == MessageDirection.INBOUND) {
            lead.unread = true
            if (lead.customerName.isNullOrBlank()) lead.customerName = email.fromName?.take(255)
        }
        lead.updatedAt = LocalDateTime.now()

        // F3: extraction runs only for freshly created leads or when key facts are missing.
        val existingFacts = factRepository.findByLeadIdOrderByIdAsc(leadId)
        if (email.direction == MessageDirection.INBOUND &&
            (existingFacts.isEmpty() || lead.vehicleBrand == null)
        ) {
            runCatching {
                extractor.extract(
                    InquiryClassificationInput(email.fromEmail, email.subject, bodyClean.take(4000))
                )
            }.onSuccess { extracted ->
                applyExtraction(account.companyId, lead, leadId, extracted, existingFacts, message)
            }.onFailure {
                logger.warn("Extraction failed for lead $leadId", it)
            }
        }
        leadRepository.save(lead)
    }

    private fun applyExtraction(
        companyId: Long,
        lead: CommLeadEntity,
        leadId: Long,
        extracted: com.carslab.crm.modules.communication.domain.ports.ExtractedInquiry,
        existingFacts: List<LeadFactEntity>,
        message: MailMessageEntity
    ) {
        if (lead.vehicleBrand == null && extracted.vehicleBrand != null) {
            lead.vehicleBrand = extracted.vehicleBrand
            lead.vehicleModel = extracted.vehicleModel
            lead.vehicleYear = extracted.vehicleYear
            val label = listOfNotNull(extracted.vehicleBrand, extracted.vehicleModel, extracted.vehicleYear?.toString())
                .joinToString(" ")
            addFactIfAbsent(companyId, leadId, LeadFactKey.VEHICLE, label, existingFacts, message.id)
        }
        if (lead.phone == null && extracted.phone != null) {
            lead.phone = extracted.phone
            addFactIfAbsent(companyId, leadId, LeadFactKey.PHONE, extracted.phone, existingFacts, message.id)
        }
        extracted.preferredDate?.let {
            addFactIfAbsent(companyId, leadId, LeadFactKey.PREFERRED_DATE, it, existingFacts, message.id)
        }
        for (service in extracted.services) {
            addFactIfAbsent(companyId, leadId, LeadFactKey.SERVICE, service, existingFacts, message.id)
        }
    }

    private fun addFactIfAbsent(
        companyId: Long,
        leadId: Long,
        key: LeadFactKey,
        value: String,
        existing: List<LeadFactEntity>,
        sourceMessageId: Long?
    ) {
        if (existing.any { it.factKey == key && it.value.equals(value, ignoreCase = true) }) return
        factRepository.save(
            LeadFactEntity(
                companyId = companyId,
                leadId = leadId,
                factKey = key,
                value = value.take(500),
                verified = false,
                sourceMessageId = sourceMessageId
            )
        )
    }
}
