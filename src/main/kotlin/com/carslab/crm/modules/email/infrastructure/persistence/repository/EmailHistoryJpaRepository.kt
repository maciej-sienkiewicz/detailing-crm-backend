package com.carslab.crm.modules.email.infrastructure.persistence.repository

import com.carslab.crm.modules.email.infrastructure.persistence.entity.EmailHistoryEntity
import com.carslab.crm.modules.email.domain.model.EmailStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EmailHistoryJpaRepository : JpaRepository<EmailHistoryEntity, String> {

    fun findByCompanyIdAndProtocolId(companyId: Long, protocolId: String): List<EmailHistoryEntity>

    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<EmailHistoryEntity>

    fun findByCompanyIdAndProtocolId(companyId: Long, protocolId: String, pageable: Pageable): Page<EmailHistoryEntity>

    @Query("SELECT e FROM EmailHistoryEntity e WHERE e.companyId = :companyId AND (:protocolId IS NULL OR e.protocolId = :protocolId) ORDER BY e.sentAt DESC")
    fun findByCompanyIdAndOptionalProtocolId(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: String?,
        pageable: Pageable
    ): Page<EmailHistoryEntity>

    fun countByCompanyIdAndStatus(companyId: Long, status: EmailStatus): Long

    fun findByCompanyIdAndSentAtBetween(
        companyId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<EmailHistoryEntity>
}