package com.carslab.crm.modules.finances.api.mappers

import com.carslab.crm.modules.finances.api.responses.BalanceHistoryPageResponse
import com.carslab.crm.modules.finances.api.responses.BalanceHistoryResponse
import com.carslab.crm.modules.finances.api.responses.BalanceStatisticsResponse
import com.carslab.crm.modules.finances.api.responses.LastOperationResponse
import com.carslab.crm.modules.finances.domain.balance.BalanceStatistics
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceHistoryEntity
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * Mapper konwertujący encje historii sald na modele odpowiedzi API
 */
@Component
class BalanceHistoryMapper {

    /**
     * Konwertuje encję na model odpowiedzi
     */
    fun toResponse(entity: BalanceHistoryEntity): BalanceHistoryResponse {
        return BalanceHistoryResponse(
            operationId = entity.id,
            balanceType = formatBalanceType(entity.balanceType),
            balanceBefore = entity.balanceBefore,
            balanceAfter = entity.balanceAfter,
            amountChanged = entity.amountChanged,
            operationType = formatOperationType(entity.operationType),
            operationDescription = entity.description,
            documentId = entity.documentId,
            userId = entity.userId,
            timestamp = entity.timestamp,
            ipAddress = entity.ipAddress,
            relatedOperationId = entity.operationId
        )
    }

    /**
     * Konwertuje stronę encji na stronę odpowiedzi
     */
    fun toPageResponse(page: Page<BalanceHistoryEntity>): BalanceHistoryPageResponse {
        return BalanceHistoryPageResponse(
            content = page.content.map { toResponse(it) },
            pageNumber = page.number,
            pageSize = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            isFirst = page.isFirst,
            isLast = page.isLast,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    /**
     * Konwertuje listę encji na listę odpowiedzi
     */
    fun toResponseList(entities: List<BalanceHistoryEntity>): List<BalanceHistoryResponse> {
        return entities.map { toResponse(it) }
    }

    /**
     * Konwertuje statystyki na model odpowiedzi
     */
    fun toStatisticsResponse(
        statistics: BalanceStatistics,
        balanceType: BalanceType,
        periodStart: LocalDateTime,
        periodEnd: LocalDateTime
    ): BalanceStatisticsResponse {
        val averageOperationSize = if (statistics.totalOperations > 0) {
            statistics.totalAmountChanged.divide(
                BigDecimal(statistics.totalOperations),
                2,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }

        return BalanceStatisticsResponse(
            periodStart = periodStart,
            periodEnd = periodEnd,
            balanceType = formatBalanceType(balanceType.name),
            totalOperations = statistics.totalOperations,
            totalAmountChanged = statistics.totalAmountChanged,
            positiveChangesCount = statistics.positiveChanges,
            negativeChangesCount = statistics.negativeChanges,
            startBalance = statistics.startBalance,
            endBalance = statistics.endBalance,
            netChange = statistics.netChange,
            averageOperationSize = averageOperationSize
        )
    }

    /**
     * Konwertuje opcjonalną encję na odpowiedź ostatniej operacji
     */
    fun toLastOperationResponse(entity: BalanceHistoryEntity?): LastOperationResponse {
        return LastOperationResponse(
            hasOperations = entity != null,
            lastOperation = entity?.let { toResponse(it) }
        )
    }

    /**
     * Formatuje typ salda dla lepszej czytelności
     */
    private fun formatBalanceType(balanceType: String): String {
        return when (balanceType.uppercase()) {
            "CASH" -> "CASH"
            "BANK" -> "BANK"
            else -> balanceType
        }
    }

    /**
     * Formatuje typ operacji dla lepszej czytelności
     */
    private fun formatOperationType(operationType: String): String {
        return when (operationType.uppercase()) {
            "ADD" -> "ADD"
            "SUBTRACT" -> "SUBTRACT"
            "CORRECTION" -> "CORRECTION"
            "MANUAL_OVERRIDE" -> "MANUAL_OVERRIDE"
            "CASH_WITHDRAWAL" -> "CASH_WITHDRAWAL"
            "CASH_DEPOSIT" -> "CASH_DEPOSIT"
            "BANK_RECONCILIATION" -> "BANK_RECONCILIATION"
            "INVENTORY_ADJUSTMENT" -> "INVENTORY_ADJUSTMENT"
            "CASH_TO_SAFE" -> "CASH_TO_SAFE"
            "CASH_FROM_SAFE" -> "CASH_FROM_SAFE"
            else -> operationType
        }
    }
}