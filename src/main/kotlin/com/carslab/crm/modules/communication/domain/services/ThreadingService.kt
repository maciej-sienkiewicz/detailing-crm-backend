package com.carslab.crm.modules.communication.domain.services

import com.carslab.crm.modules.communication.domain.model.MessageDirection
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailThreadEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.MailMessageJpaRepository
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.MailThreadJpaRepository
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime

object SubjectNormalizer {

    private val replyPrefix = Regex(
        "^\\s*((re|odp|odpowiedz|fwd|fw|pd)\\s*(\\[\\d+])?\\s*:\\s*)+",
        RegexOption.IGNORE_CASE
    )

    fun normalize(subject: String?): String? {
        if (subject.isNullOrBlank()) return null
        var s = subject.trim()
        while (true) {
            val stripped = replyPrefix.replaceFirst(s, "").trim()
            if (stripped == s) break
            s = stripped
        }
        return s.lowercase().replace(Regex("\\s+"), " ").take(500).ifBlank { null }
    }
}

/**
 * Resolves the thread for an incoming message using a deterministic cascade
 * (a simplified JWZ algorithm), never the "open lead for this address" heuristic:
 * 1. In-Reply-To matches a known Message-ID
 * 2. Any References id matches, newest first
 * 3. Provider thread key (X-GM-THRID / Graph conversationId)
 * 4. Same sender + normalized subject within 30 days
 * 5. New thread
 */
@Service
class ThreadingService(
    private val threadRepository: MailThreadJpaRepository,
    private val messageRepository: MailMessageJpaRepository
) {

    fun resolveThread(
        companyId: Long,
        mailAccountId: Long,
        messageIdHeader: String?,
        inReplyTo: String?,
        references: List<String>,
        externalThreadKey: String?,
        counterpartyEmail: String,
        subject: String?,
        sentAt: LocalDateTime
    ): MailThreadEntity {
        val subjectNorm = SubjectNormalizer.normalize(subject)

        inReplyTo?.let { candidate ->
            messageRepository.findFirstByCompanyIdAndMessageId(companyId, candidate)?.let {
                return threadRepository.getReferenceById(it.threadId)
            }
        }

        for (ref in references.asReversed()) {
            messageRepository.findFirstByCompanyIdAndMessageId(companyId, ref)?.let {
                return threadRepository.getReferenceById(it.threadId)
            }
        }

        externalThreadKey?.let { key ->
            threadRepository.findByCompanyIdAndExternalThreadKey(companyId, key)?.let { return it }
        }

        if (subjectNorm != null) {
            threadRepository.findRecentBySubjectAndParticipant(
                companyId, subjectNorm, counterpartyEmail, sentAt.minusDays(30)
            ).firstOrNull()?.let { return it }
        }

        return threadRepository.save(
            MailThreadEntity(
                companyId = companyId,
                mailAccountId = mailAccountId,
                subjectNorm = subjectNorm,
                externalThreadKey = externalThreadKey,
                lastMessageAt = sentAt
            )
        )
    }

    fun touchThread(thread: MailThreadEntity, sentAt: LocalDateTime, direction: MessageDirection) {
        if (thread.lastMessageAt <= sentAt) {
            thread.lastMessageAt = sentAt
            thread.lastDirection = direction
        }
        threadRepository.save(thread)
    }

    companion object {
        /** Normalizes a raw Message-ID header value: trims whitespace and angle brackets. */
        fun normalizeMessageId(raw: String?): String? =
            raw?.trim()?.removePrefix("<")?.removeSuffix(">")?.trim()?.take(500)?.ifBlank { null }

        fun parseReferences(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()
            return Regex("<([^<>]+)>").findAll(raw).map { it.groupValues[1].take(500) }.toList()
        }

        /**
         * Deterministic synthetic Message-ID for messages that arrive without one,
         * so re-ingestion stays idempotent.
         */
        fun syntheticMessageId(fromEmail: String, sentAt: LocalDateTime, subject: String?): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest("$fromEmail|$sentAt|${subject.orEmpty()}".toByteArray())
                .joinToString("") { "%02x".format(it) }
            return "synthetic-$digest@carslab.local"
        }
    }
}
