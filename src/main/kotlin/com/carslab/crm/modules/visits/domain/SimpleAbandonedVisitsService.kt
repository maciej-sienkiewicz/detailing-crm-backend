package com.carslab.crm.modules.visits.domain

import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.modules.visits.api.response.AbandonedVisitsCleanupResult
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class SimpleAbandonedVisitsService(
    private val entityManager: EntityManager
) {

    private val logger = LoggerFactory.getLogger(SimpleAbandonedVisitsService::class.java)

    fun cancelAbandonedVisits(): AbandonedVisitsCleanupResult {
        val yesterday = LocalDate.now().minusDays(1)
        val cleanupTimestamp = LocalDateTime.now()

        logger.info("🔍 Looking for abandoned visits from $yesterday...")

        // Step 1: Single query to update all matching protocols
        val updatedProtocolsCount = updateAbandonedProtocols(yesterday, cleanupTimestamp)

        // Step 2: Add comments for all updated protocols
        val commentsAddedCount = addCommentsToUpdatedProtocols(yesterday, cleanupTimestamp)

        return AbandonedVisitsCleanupResult(
            updatedProtocolsCount = updatedProtocolsCount,
            commentsAddedCount = commentsAddedCount,
            processedDate = yesterday,
            executionTimestamp = cleanupTimestamp
        )
    }

    private fun updateAbandonedProtocols(targetDate: LocalDate, timestamp: LocalDateTime): Int {
        // Native SQL query using actual table structure from ProtocolEntity
        val updateQuery = """
            UPDATE protocols 
            SET status = :cancelledStatus,
                updated_at = :timestamp,
                status_updated_at = :timestamp
            WHERE DATE(start_date) = :targetDate 
            AND status = :scheduledStatus
        """.trimIndent()

        val updatedCount = entityManager.createNativeQuery(updateQuery)
            .setParameter("cancelledStatus", ProtocolStatus.CANCELLED.name)
            .setParameter("scheduledStatus", ProtocolStatus.SCHEDULED.name)
            .setParameter("targetDate", targetDate)
            .setParameter("timestamp", timestamp)
            .executeUpdate()

        logger.info("📊 Updated $updatedCount protocols to CANCELLED status")
        return updatedCount
    }

    private fun addCommentsToUpdatedProtocols(targetDate: LocalDate, timestamp: LocalDateTime): Int {
        // Get all protocol IDs that were just updated
        val getUpdatedProtocolsQuery = """
            SELECT id 
            FROM protocols 
            WHERE DATE(start_date) = :targetDate 
            AND status = :cancelledStatus
            AND updated_at = :timestamp
        """.trimIndent()

        val protocolIds = entityManager.createNativeQuery(getUpdatedProtocolsQuery)
            .setParameter("targetDate", targetDate)
            .setParameter("cancelledStatus", ProtocolStatus.CANCELLED.name)
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

        // Batch insert comments using actual ProtocolCommentEntity structure
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

        val commentTimestamp = timestamp.toString() // String format as required by entity

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