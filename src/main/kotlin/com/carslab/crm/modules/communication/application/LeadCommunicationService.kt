package com.carslab.crm.modules.communication.application

import com.carslab.crm.modules.communication.api.dto.FactDto
import com.carslab.crm.modules.communication.api.dto.LeadCardDto
import com.carslab.crm.modules.communication.api.dto.LeadDetailDto
import com.carslab.crm.modules.communication.api.dto.QuoteItemDto
import com.carslab.crm.modules.communication.api.dto.ReplyRequest
import com.carslab.crm.modules.communication.api.dto.ReviewItemDto
import com.carslab.crm.modules.communication.api.dto.TimelineItemDto
import com.carslab.crm.modules.communication.domain.model.LeadFactKey
import com.carslab.crm.modules.communication.domain.model.LeadStage
import com.carslab.crm.modules.communication.domain.model.MessageDirection
import com.carslab.crm.modules.communication.domain.model.MessageSendStatus
import com.carslab.crm.modules.communication.domain.model.ThreadClassification
import com.carslab.crm.modules.communication.domain.services.ThreadingService
import com.carslab.crm.modules.communication.infrastructure.mail.OutgoingReply
import com.carslab.crm.modules.communication.infrastructure.mail.ThreadAwareSmtpSender
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.CommLeadEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.LeadEventEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.LeadNoteEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.LeadQuoteEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailMessageEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.SenderRuleEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.CommLeadJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.LeadEventJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.LeadFactJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.LeadNoteJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.LeadQuoteJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.MailAccountJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.MailMessageJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.MailThreadJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.SenderRuleJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LeadNotFoundException(message: String) : RuntimeException(message)

@Service
class LeadCommunicationService(
    private val leadRepository: CommLeadJpaRepository,
    private val factRepository: LeadFactJpaRepository,
    private val quoteRepository: LeadQuoteJpaRepository,
    private val noteRepository: LeadNoteJpaRepository,
    private val eventRepository: LeadEventJpaRepository,
    private val threadRepository: MailThreadJpaRepository,
    private val messageRepository: MailMessageJpaRepository,
    private val accountRepository: MailAccountJpaRepository,
    private val senderRuleRepository: SenderRuleJpaRepository,
    private val smtpSender: ThreadAwareSmtpSender,
    private val objectMapper: ObjectMapper
) {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun listLeads(companyId: Long, stage: LeadStage?): List<LeadCardDto> {
        val leads = if (stage != null) {
            leadRepository.findByCompanyIdAndStageOrderByUpdatedAtDesc(companyId, stage)
        } else {
            leadRepository.findByCompanyIdOrderByUpdatedAtDesc(companyId)
        }
        return leads.map { toCard(it) }
    }

    fun getLeadDetail(companyId: Long, leadId: Long): LeadDetailDto {
        val lead = leadRepository.findByIdAndCompanyId(leadId, companyId)
            ?: throw LeadNotFoundException("Lead $leadId not found")
        val facts = factRepository.findByLeadIdOrderByIdAsc(leadId)
        val card = toCard(lead)

        val timeline = buildTimeline(lead)

        return LeadDetailDto(
            id = card.id,
            stage = card.stage,
            vehicleBrand = card.vehicleBrand,
            vehicleModel = card.vehicleModel,
            vehicleYear = card.vehicleYear,
            services = card.services,
            customerName = card.customerName,
            contactEmail = card.contactEmail,
            phone = lead.phone,
            quoteTotal = card.quoteTotal,
            lastMessageAt = card.lastMessageAt,
            lastDirection = card.lastDirection,
            unread = card.unread,
            returningCustomer = card.returningCustomer,
            createdAt = card.createdAt,
            facts = facts.map {
                FactDto(requireNotNull(it.id), it.factKey.name, it.value, it.verified, it.sourceMessageId)
            },
            timeline = timeline
        )
    }

    @Transactional
    fun markRead(companyId: Long, leadId: Long) {
        val lead = leadRepository.findByIdAndCompanyId(leadId, companyId) ?: return
        if (lead.unread) {
            lead.unread = false
            leadRepository.save(lead)
        }
    }

    /**
     * Sends the reply through the owner's own SMTP account with deterministic
     * threading headers. Transactional outbox: the OUTBOUND row (with our
     * generated Message-ID) is persisted as OUTBOX_PENDING before the SMTP
     * call, then promoted to SENT / FAILED.
     */
    @Transactional
    fun reply(companyId: Long, leadId: Long, request: ReplyRequest, authorName: String?): Long {
        val lead = leadRepository.findByIdAndCompanyId(leadId, companyId)
            ?: throw LeadNotFoundException("Lead $leadId not found")
        val thread = threadRepository.findByIdAndCompanyId(lead.threadId, companyId)
            ?: throw LeadNotFoundException("Thread ${lead.threadId} not found")
        val account = accountRepository.findByIdAndCompanyId(thread.mailAccountId, companyId)
            ?: throw IllegalStateException("Mail account of thread ${thread.id} not found")

        val messages = messageRepository.findByThreadIdOrderBySentAtAsc(lead.threadId)
        val lastMessage = messages.lastOrNull()
        val firstSubject = messages.firstOrNull()?.subject ?: "Zapytanie"
        val subject = if (firstSubject.startsWith("Re:", ignoreCase = true)) firstSubject else "Re: $firstSubject"

        val referencesChain = (messages.flatMap { listOf(it.messageId) }).distinct().takeLast(10)

        val quoteHtml = request.quote?.let { renderQuoteHtml(it.items) } ?: ""
        val bodyHtml = "<div>" + escapeHtml(request.bodyText).replace("\n", "<br/>") + "</div>" + quoteHtml

        val newMessageId = smtpSender.newMessageId(account)
        val now = LocalDateTime.now()

        val outbound = messageRepository.save(
            MailMessageEntity(
                companyId = companyId,
                threadId = lead.threadId,
                mailAccountId = requireNotNull(account.id),
                direction = MessageDirection.OUTBOUND,
                messageId = newMessageId,
                inReplyTo = lastMessage?.messageId,
                referencesIds = referencesChain.joinToString(" ").ifBlank { null },
                fromEmail = account.emailAddress,
                fromName = authorName,
                toEmails = lead.contactEmail,
                subject = subject,
                sentAt = now,
                bodyHtml = bodyHtml,
                bodyTextClean = request.bodyText.take(4000),
                sendStatus = MessageSendStatus.OUTBOX_PENDING
            )
        )

        try {
            smtpSender.send(
                account, newMessageId,
                OutgoingReply(
                    to = lead.contactEmail,
                    subject = subject,
                    bodyText = request.bodyText,
                    bodyHtml = bodyHtml,
                    inReplyTo = lastMessage?.messageId,
                    references = referencesChain
                )
            )
            outbound.sendStatus = MessageSendStatus.SENT
        } catch (e: Exception) {
            outbound.sendStatus = MessageSendStatus.FAILED
            messageRepository.save(outbound)
            throw IllegalStateException("Wysyłka nie powiodła się: ${e.message}", e)
        }
        messageRepository.save(outbound)

        thread.lastMessageAt = now
        thread.lastDirection = MessageDirection.OUTBOUND
        if (thread.classification == ThreadClassification.PENDING || thread.classification == ThreadClassification.UNSURE) {
            thread.classification = ThreadClassification.INQUIRY
        }
        threadRepository.save(thread)

        request.quote?.let { quote ->
            val total = quote.items.sumOf { it.price }
            quoteRepository.save(
                LeadQuoteEntity(
                    companyId = companyId,
                    leadId = leadId,
                    messageId = outbound.id,
                    itemsJson = objectMapper.writeValueAsString(quote.items),
                    total = total,
                    sentAt = now
                )
            )
            lead.quoteTotal = total
            lead.quoteSentAt = now
        }

        val nextStage = request.nextStage?.let { runCatching { LeadStage.valueOf(it) }.getOrNull() }
            ?: if (request.quote != null) LeadStage.QUOTED else null
        if (nextStage != null && nextStage != lead.stage) {
            changeStageInternal(lead, nextStage, null)
        }
        lead.unread = false
        lead.updatedAt = now
        leadRepository.save(lead)

        // Using facts in an action verifies them implicitly.
        if (request.quote != null) verifyAllFacts(leadId)

        return requireNotNull(outbound.id)
    }

    @Transactional
    fun changeStage(companyId: Long, leadId: Long, stage: LeadStage, reason: String?) {
        val lead = leadRepository.findByIdAndCompanyId(leadId, companyId)
            ?: throw LeadNotFoundException("Lead $leadId not found")
        changeStageInternal(lead, stage, reason)
        lead.updatedAt = LocalDateTime.now()
        leadRepository.save(lead)
    }

    private fun changeStageInternal(lead: CommLeadEntity, stage: LeadStage, reason: String?) {
        if (lead.stage == stage) return
        lead.stage = stage
        if (stage == LeadStage.LOST) lead.lostReason = reason
        eventRepository.save(
            LeadEventEntity(
                companyId = lead.companyId,
                leadId = requireNotNull(lead.id),
                text = "→ ${stageLabel(stage)}" + (reason?.let { " · $it" } ?: "")
            )
        )
    }

    @Transactional
    fun addNote(companyId: Long, leadId: Long, text: String, author: String?): TimelineItemDto {
        leadRepository.findByIdAndCompanyId(leadId, companyId)
            ?: throw LeadNotFoundException("Lead $leadId not found")
        val note = noteRepository.save(
            LeadNoteEntity(companyId = companyId, leadId = leadId, text = text, author = author)
        )
        return TimelineItemDto(
            type = "NOTE",
            id = note.id,
            text = note.text,
            createdAt = note.createdAt.format(isoFormatter),
            author = note.author
        )
    }

    @Transactional
    fun updateFact(companyId: Long, leadId: Long, factId: Long, value: String): FactDto {
        val fact = factRepository.findByIdAndCompanyId(factId, companyId)
            ?: throw LeadNotFoundException("Fact $factId not found")
        require(fact.leadId == leadId) { "Fact does not belong to lead $leadId" }
        fact.value = value.take(500)
        fact.verified = true
        factRepository.save(fact)
        syncLeadFromFacts(companyId, leadId)
        return FactDto(requireNotNull(fact.id), fact.factKey.name, fact.value, fact.verified, fact.sourceMessageId)
    }

    @Transactional
    fun verifyAllFacts(leadId: Long) {
        val facts = factRepository.findByLeadIdOrderByIdAsc(leadId)
        facts.filter { !it.verified }.forEach {
            it.verified = true
            factRepository.save(it)
        }
    }

    fun listReviewQueue(companyId: Long): List<ReviewItemDto> {
        val threads = threadRepository.findByCompanyIdAndClassificationOrderByLastMessageAtDesc(
            companyId, ThreadClassification.UNSURE
        )
        return threads.mapNotNull { thread ->
            val threadId = thread.id ?: return@mapNotNull null
            val first = messageRepository.findFirstByThreadIdOrderBySentAtAsc(threadId) ?: return@mapNotNull null
            ReviewItemDto(
                threadId = threadId,
                fromEmail = first.fromEmail,
                fromName = first.fromName,
                subject = first.subject,
                snippet = (first.bodyTextClean ?: "").take(200),
                receivedAt = first.sentAt.format(isoFormatter)
            )
        }
    }

    @Transactional
    fun reviewDecision(companyId: Long, threadId: Long, decision: String) {
        val thread = threadRepository.findByIdAndCompanyId(threadId, companyId)
            ?: throw LeadNotFoundException("Thread $threadId not found")
        val first = messageRepository.findFirstByThreadIdOrderBySentAtAsc(threadId)

        when (decision.uppercase()) {
            "INQUIRY" -> {
                thread.classification = ThreadClassification.INQUIRY
                threadRepository.save(thread)
                if (leadRepository.findByThreadId(threadId) == null && first != null) {
                    leadRepository.save(
                        CommLeadEntity(
                            companyId = companyId,
                            threadId = threadId,
                            contactEmail = first.fromEmail,
                            customerName = first.fromName
                        )
                    )
                }
                first?.let { saveSenderRule(companyId, it.fromEmail, ThreadClassification.INQUIRY) }
            }
            "IGNORE_SENDER" -> {
                thread.classification = ThreadClassification.OTHER
                threadRepository.save(thread)
                first?.let { saveSenderRule(companyId, it.fromEmail, ThreadClassification.OTHER) }
            }
            else -> throw IllegalArgumentException("Unknown decision: $decision")
        }
    }

    private fun saveSenderRule(companyId: Long, email: String, classification: ThreadClassification) {
        val pattern = email.lowercase()
        if (senderRuleRepository.findByCompanyIdAndPatternIn(companyId, listOf(pattern)).isEmpty()) {
            senderRuleRepository.save(
                SenderRuleEntity(companyId = companyId, pattern = pattern, classification = classification)
            )
        }
    }

    private fun buildTimeline(lead: CommLeadEntity): List<TimelineItemDto> {
        val leadId = requireNotNull(lead.id)
        val items = mutableListOf<Pair<LocalDateTime, TimelineItemDto>>()

        for (m in messageRepository.findByThreadIdOrderBySentAtAsc(lead.threadId)) {
            items.add(
                m.sentAt to TimelineItemDto(
                    type = "MESSAGE",
                    id = m.id,
                    direction = m.direction.name,
                    sentAt = m.sentAt.format(isoFormatter),
                    fromName = m.fromName,
                    bodyText = m.bodyTextClean ?: ""
                )
            )
        }
        for (n in noteRepository.findByLeadIdOrderByCreatedAtAsc(leadId)) {
            items.add(
                n.createdAt to TimelineItemDto(
                    type = "NOTE", id = n.id, text = n.text,
                    createdAt = n.createdAt.format(isoFormatter), author = n.author
                )
            )
        }
        for (e in eventRepository.findByLeadIdOrderByCreatedAtAsc(leadId)) {
            items.add(
                e.createdAt to TimelineItemDto(type = "EVENT", text = e.text, at = e.createdAt.format(isoFormatter))
            )
        }
        for (q in quoteRepository.findByLeadIdOrderBySentAtAsc(leadId)) {
            val quoteItems: List<QuoteItemDto> = runCatching {
                objectMapper.readValue<List<QuoteItemDto>>(q.itemsJson)
            }.getOrDefault(emptyList())
            items.add(
                q.sentAt to TimelineItemDto(
                    type = "QUOTE", id = q.id, items = quoteItems, total = q.total,
                    sentAt = q.sentAt.format(isoFormatter)
                )
            )
        }
        return items.sortedBy { it.first }.map { it.second }
    }

    private fun toCard(lead: CommLeadEntity): LeadCardDto {
        val leadId = requireNotNull(lead.id)
        val thread = threadRepository.findById(lead.threadId).orElse(null)
        val services = factRepository.findByLeadIdOrderByIdAsc(leadId)
            .filter { it.factKey == LeadFactKey.SERVICE }
            .map { it.value }
        val returning = leadRepository.countByCompanyIdAndContactEmailAndIdNot(
            lead.companyId, lead.contactEmail, leadId
        ) > 0
        return LeadCardDto(
            id = leadId,
            stage = lead.stage.name,
            vehicleBrand = lead.vehicleBrand,
            vehicleModel = lead.vehicleModel,
            vehicleYear = lead.vehicleYear,
            services = services,
            customerName = lead.customerName,
            contactEmail = lead.contactEmail,
            quoteTotal = lead.quoteTotal,
            lastMessageAt = (thread?.lastMessageAt ?: lead.updatedAt).format(isoFormatter),
            lastDirection = (thread?.lastDirection ?: MessageDirection.INBOUND).name,
            unread = lead.unread,
            returningCustomer = returning,
            createdAt = lead.createdAt.format(isoFormatter)
        )
    }

    private fun renderQuoteHtml(items: List<QuoteItemDto>): String {
        val rows = items.joinToString("") {
            "<tr><td style=\"padding:6px 12px;border-bottom:1px solid #e5e7eb;\">${escapeHtml(it.name)}</td>" +
                "<td style=\"padding:6px 12px;border-bottom:1px solid #e5e7eb;text-align:right;white-space:nowrap;\">${formatPln(it.price)}</td></tr>"
        }
        val total = items.sumOf { it.price }
        return """
            <table style="border-collapse:collapse;margin:12px 0;min-width:320px;font-family:Arial,sans-serif;font-size:14px;">
              <thead><tr>
                <th style="text-align:left;padding:6px 12px;border-bottom:2px solid #1a1a1a;">Usługa</th>
                <th style="text-align:right;padding:6px 12px;border-bottom:2px solid #1a1a1a;">Cena</th>
              </tr></thead>
              <tbody>$rows</tbody>
              <tfoot><tr>
                <td style="padding:8px 12px;font-weight:bold;">Razem</td>
                <td style="padding:8px 12px;font-weight:bold;text-align:right;white-space:nowrap;">${formatPln(total)}</td>
              </tr></tfoot>
            </table>
        """.trimIndent()
    }

    private fun formatPln(grosze: Long): String =
        "%,.2f zł".format(grosze / 100.0).replace(',', ' ').replace('.', ',')

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun syncLeadFromFacts(companyId: Long, leadId: Long) {
        val lead = leadRepository.findByIdAndCompanyId(leadId, companyId) ?: return
        val facts = factRepository.findByLeadIdOrderByIdAsc(leadId)
        facts.firstOrNull { it.factKey == LeadFactKey.PHONE }?.let { lead.phone = it.value }
        facts.firstOrNull { it.factKey == LeadFactKey.VEHICLE }?.let { vehicleFact ->
            val parts = vehicleFact.value.split(" ")
            if (parts.isNotEmpty()) lead.vehicleBrand = parts.first()
            if (parts.size > 1) lead.vehicleModel = parts.drop(1).filter { it.toIntOrNull() == null }
                .joinToString(" ").ifBlank { lead.vehicleModel }
            parts.lastOrNull()?.toIntOrNull()?.let { lead.vehicleYear = it }
        }
        lead.updatedAt = LocalDateTime.now()
        leadRepository.save(lead)
    }

    private fun stageLabel(stage: LeadStage): String = when (stage) {
        LeadStage.NEW -> "Nowe"
        LeadStage.QUOTED -> "Wycenione"
        LeadStage.TO_CONFIRM -> "Do potwierdzenia"
        LeadStage.SCHEDULED -> "Umówione"
        LeadStage.WON -> "Wygrane"
        LeadStage.LOST -> "Przegrane"
    }
}
