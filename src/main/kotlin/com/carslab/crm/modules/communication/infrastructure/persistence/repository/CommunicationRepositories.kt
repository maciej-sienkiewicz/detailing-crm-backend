package com.carslab.crm.modules.communication.infrastructure.persistence.repository

import com.carslab.crm.modules.communication.domain.model.LeadStage
import com.carslab.crm.modules.communication.domain.model.MailAccountStatus
import com.carslab.crm.modules.communication.domain.model.MailProviderType
import com.carslab.crm.modules.communication.domain.model.ThreadClassification
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.CommLeadEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.LeadEventEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.LeadFactEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.LeadNoteEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.LeadQuoteEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailAccountEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailMessageEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailThreadEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.SenderRuleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MailAccountJpaRepository : JpaRepository<MailAccountEntity, Long> {
    fun findByCompanyId(companyId: Long): List<MailAccountEntity>
    fun findByCompanyIdAndEmailAddress(companyId: Long, emailAddress: String): MailAccountEntity?
    fun findByStatusAndProviderType(status: MailAccountStatus, providerType: MailProviderType): List<MailAccountEntity>
    fun findByIdAndCompanyId(id: Long, companyId: Long): MailAccountEntity?
}

@Repository
interface MailThreadJpaRepository : JpaRepository<MailThreadEntity, Long> {
    fun findByIdAndCompanyId(id: Long, companyId: Long): MailThreadEntity?
    fun findByCompanyIdAndExternalThreadKey(companyId: Long, externalThreadKey: String): MailThreadEntity?
    fun findByCompanyIdAndClassificationOrderByLastMessageAtDesc(
        companyId: Long,
        classification: ThreadClassification
    ): List<MailThreadEntity>

    @Query(
        """SELECT t FROM MailThreadEntity t
           WHERE t.companyId = :companyId AND t.subjectNorm = :subjectNorm
             AND t.lastMessageAt > :since
             AND EXISTS (
                SELECT m FROM MailMessageEntity m
                WHERE m.threadId = t.id AND m.fromEmail = :fromEmail
             )
           ORDER BY t.lastMessageAt DESC"""
    )
    fun findRecentBySubjectAndParticipant(
        @Param("companyId") companyId: Long,
        @Param("subjectNorm") subjectNorm: String,
        @Param("fromEmail") fromEmail: String,
        @Param("since") since: LocalDateTime
    ): List<MailThreadEntity>
}

@Repository
interface MailMessageJpaRepository : JpaRepository<MailMessageEntity, Long> {
    fun findByMailAccountIdAndMessageId(mailAccountId: Long, messageId: String): MailMessageEntity?
    fun findFirstByCompanyIdAndMessageId(companyId: Long, messageId: String): MailMessageEntity?
    fun findByThreadIdOrderBySentAtAsc(threadId: Long): List<MailMessageEntity>
    fun findFirstByThreadIdOrderBySentAtDesc(threadId: Long): MailMessageEntity?
    fun findFirstByThreadIdOrderBySentAtAsc(threadId: Long): MailMessageEntity?
}

@Repository
interface CommLeadJpaRepository : JpaRepository<CommLeadEntity, Long> {
    fun findByIdAndCompanyId(id: Long, companyId: Long): CommLeadEntity?
    fun findByThreadId(threadId: Long): CommLeadEntity?
    fun findByCompanyIdOrderByUpdatedAtDesc(companyId: Long): List<CommLeadEntity>
    fun findByCompanyIdAndStageOrderByUpdatedAtDesc(companyId: Long, stage: LeadStage): List<CommLeadEntity>
    fun countByCompanyIdAndContactEmailAndIdNot(companyId: Long, contactEmail: String, id: Long): Long

    @Query(
        """SELECT l FROM CommLeadEntity l
           WHERE l.companyId = :companyId AND l.stage = :stage
             AND l.quoteSentAt IS NOT NULL AND l.quoteSentAt < :before"""
    )
    fun findQuotedAndSilentSince(
        @Param("companyId") companyId: Long,
        @Param("stage") stage: LeadStage,
        @Param("before") before: LocalDateTime
    ): List<CommLeadEntity>
}

@Repository
interface LeadFactJpaRepository : JpaRepository<LeadFactEntity, Long> {
    fun findByLeadIdOrderByIdAsc(leadId: Long): List<LeadFactEntity>
    fun findByIdAndCompanyId(id: Long, companyId: Long): LeadFactEntity?
}

@Repository
interface LeadQuoteJpaRepository : JpaRepository<LeadQuoteEntity, Long> {
    fun findByLeadIdOrderBySentAtAsc(leadId: Long): List<LeadQuoteEntity>
}

@Repository
interface LeadNoteJpaRepository : JpaRepository<LeadNoteEntity, Long> {
    fun findByLeadIdOrderByCreatedAtAsc(leadId: Long): List<LeadNoteEntity>
}

@Repository
interface LeadEventJpaRepository : JpaRepository<LeadEventEntity, Long> {
    fun findByLeadIdOrderByCreatedAtAsc(leadId: Long): List<LeadEventEntity>
}

@Repository
interface SenderRuleJpaRepository : JpaRepository<SenderRuleEntity, Long> {
    fun findByCompanyIdAndPatternIn(companyId: Long, patterns: Collection<String>): List<SenderRuleEntity>
}
