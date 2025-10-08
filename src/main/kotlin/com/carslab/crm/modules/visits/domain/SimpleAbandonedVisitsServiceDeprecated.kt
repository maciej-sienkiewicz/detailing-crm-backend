package com.carslab.crm.modules.visits.domain

import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.modules.visits.api.response.AbandonedVisitsCleanupResult
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class SimpleAbandonedVisitsServiceDeprecated(
    private val entityManager: EntityManager
) {

    private val logger = LoggerFactory.getLogger(SimpleAbandonedVisitsServiceDeprecated::class.java)

    fun cancelAbandonedVisits(): AbandonedVisitsCleanupResult {
        val yesterday = LocalDate.now().minusDays(1)
        val cleanupTimestamp = LocalDateTime.now()

        logger.info("🔍 Looking for abandoned visitss from $yesterday...")

        val updatedVisitsCount = updateAbandonedVisits(yesterday, cleanupTimestamp)
        updateAbandonedVisits(yesterday.minusDays(1), cleanupTimestamp)

        return AbandonedVisitsCleanupResult(
            updatedProtocolsCount = updatedVisitsCount,
            commentsAddedCount = 0,
            processedDate = yesterday,
            executionTimestamp = cleanupTimestamp
        )
    }

    private fun updateAbandonedVisits(targetDate: LocalDate, timestamp: LocalDateTime): Int {
        val updateQuery = """
            UPDATE visits 
            SET status = :cancelledStatus,
                updated_at = :timestamp
            WHERE CAST(start_date AS DATE) = CAST(:targetDate AS DATE)
            AND status = :scheduledStatus
        """.trimIndent()

        val updatedCount = entityManager.createNativeQuery(updateQuery)
            .setParameter("cancelledStatus", VisitStatus.CANCELLED.name)
            .setParameter("scheduledStatus", VisitStatus.SCHEDULED.name)
            .setParameter("targetDate", java.sql.Date.valueOf(targetDate))
            .setParameter("timestamp", timestamp)
            .executeUpdate()

        logger.info("📊 Updated $updatedCount visits to CANCELLED status")
        return updatedCount
    }

    private fun addCommentsToUpdatedProtocols(targetDate: LocalDate, timestamp: LocalDateTime): Int {
        val getUpdatedProtocolsQuery = """
            SELECT id 
            FROM visits 
            WHERE CAST(start_date AS DATE) = CAST(:targetDate AS DATE)
            AND status = :cancelledStatus
            AND updated_at = :timestamp
        """.trimIndent()

        val protocolIds = entityManager.createNativeQuery(getUpdatedProtocolsQuery)
            .setParameter("targetDate", java.sql.Date.valueOf(targetDate))
            .setParameter("cancelledStatus", VisitStatus.CANCELLED.name)
            .setParameter("timestamp", timestamp)
            .resultList
            .map {
                when (it) {
                    is Number -> it.toLong()
                    else -> it.toString().toLong()
                }
            }

        logger.info("🔍 Found ${protocolIds.size} protocols to add comments to")

        if (protocolIds.isEmpty()) {
            return 0
        }

        val insertCommentsQuery = """
            INSERT INTO protocol_comments (
                protocol_id, 
                author, 
                content, 
                timestamp, 
                type, 
                created_at
            ) VALUES (
                :protocolId, 
                :author, 
                :content, 
                :timestamp, 
                :type, 
                :createdAt
            )
        """.trimIndent()

        val commentTimestamp = timestamp.toString()

        var commentsAdded = 0
        protocolIds.forEach { protocolId ->
            try {
                entityManager.createNativeQuery(insertCommentsQuery)
                    .setParameter("protocolId", protocolId)
                    .setParameter("author", "SYSTEM")
                    .setParameter("content", "Wizyta automatycznie anulowana - klient nie stawił się w wyznaczonym terminie ($targetDate)")
                    .setParameter("timestamp", commentTimestamp)
                    .setParameter("type", "SYSTEM_AUTO_CANCELLATION")
                    .setParameter("createdAt", timestamp)
                    .executeUpdate()

                commentsAdded++
            } catch (exception: Exception) {
                logger.warn("⚠️ Failed to add comment for protocol $protocolId", exception)
            }
        }

        logger.info("💬 Added $commentsAdded comments successfully")
        return commentsAdded
    }
}