package com.carslab.crm.modules.finances.domain.balance

import com.carslab.crm.modules.finances.infrastructure.entity.BalanceHistoryEntity
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceOperationType
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import com.carslab.crm.modules.finances.infrastructure.repository.BalanceHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Serwis zarządzający historią zmian sald - PRODUKCYJNA WERSJA
 * Używa Criteria API jako głównej metody z fallback do prostych zapytań
 */
@Service
@Transactional
class BalanceHistoryService(
    private val balanceHistoryRepository: BalanceHistoryRepository
) {

    private val logger = LoggerFactory.getLogger(BalanceHistoryService::class.java)

    /**
     * Zapisuje zmianę salda do historii - używa REQUIRES_NEW dla niezależności
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordBalanceChange(
        companyId: Long,
        balanceType: BalanceType,
        balanceBefore: BigDecimal,
        balanceAfter: BigDecimal,
        operationType: BalanceOperationType,
        description: String,
        userId: String,
        documentId: String? = null,
        operationId: Long? = null,
        ipAddress: String? = null,
        metadata: Map<String, Any>? = null
    ) {
        try {
            val historyEntry = BalanceHistoryEntity(
                companyId = companyId,
                balanceType = balanceType.name,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                amountChanged = balanceAfter - balanceBefore,
                operationType = operationType.name,
                description = description,
                documentId = documentId,
                operationId = operationId,
                userId = userId,
                timestamp = LocalDateTime.now(),
                ipAddress = ipAddress,
                metadata = metadata?.let { convertMapToJson(it) }
            )

            balanceHistoryRepository.save(historyEntry)

            logger.debug("Balance history recorded: company=$companyId, type=$balanceType, " +
                    "before=$balanceBefore, after=$balanceAfter, operation=$operationType")

        } catch (e: Exception) {
            logger.error("Failed to record balance history for company $companyId", e)
            // Nie rzucamy wyjątku - historia jest nice-to-have
        }
    }

    /**
     * GŁÓWNA METODA - Wyszukuje historię z zaawansowanymi filtrami
     * Używa Criteria API z fallback do prostych metod
     */
    @Transactional(readOnly = true)
    fun searchBalanceHistory(
        companyId: Long,
        balanceType: BalanceType? = null,
        operationType: BalanceOperationType? = null,
        userId: String? = null,
        documentId: String? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        searchText: String? = null,
        pageable: Pageable
    ): Page<BalanceHistoryEntity> {
        return try {
            logger.debug("Searching balance history with Criteria API")

            // GŁÓWNA METODA - Criteria API (type-safe, database-agnostic)
            balanceHistoryRepository.searchWithCriteria(
                companyId = companyId,
                balanceType = balanceType?.name,
                operationType = operationType?.name,
                userId = userId,
                documentId = documentId,
                startDate = startDate,
                endDate = endDate,
                searchText = searchText,
                pageable = pageable
            )

        } catch (e: Exception) {
            logger.warn("Criteria API search failed, using fallback: ${e.message}")

            // FALLBACK - proste metody JPA
            when {
                // Jeśli tylko typ salda
                balanceType != null && otherFiltersEmpty(operationType, userId, documentId, startDate, endDate, searchText) -> {
                    balanceHistoryRepository.findByCompanyIdAndBalanceTypeOrderByTimestampDesc(
                        companyId, balanceType.name, pageable
                    )
                }
                // Jeśli tylko użytkownik
                userId != null && otherFiltersEmpty(balanceType, operationType, null, documentId, startDate, endDate, searchText) -> {
                    balanceHistoryRepository.findByCompanyIdAndUserIdOrderByTimestampDesc(
                        companyId, userId, pageable
                    )
                }
                // Jeśli tylko przedział czasowy
                startDate != null && endDate != null && otherFiltersEmpty(balanceType, operationType, userId, documentId, null, null, searchText) -> {
                    balanceHistoryRepository.findByCompanyIdAndTimestampBetweenOrderByTimestampDesc(
                        companyId, startDate, endDate, pageable
                    )
                }
                // Ostatnia deska ratunku - wszystkie dane
                else -> {
                    logger.info("Using basic fallback - returning all data for company $companyId")
                    balanceHistoryRepository.findByCompanyIdOrderByTimestampDesc(companyId, pageable)
                }
            }
        }
    }

    /**
     * Pobiera historię sald dla firmy z paginacją - PROSTA METODA
     */
    @Transactional(readOnly = true)
    fun getBalanceHistory(
        companyId: Long,
        pageable: Pageable
    ): Page<BalanceHistoryEntity> {
        return balanceHistoryRepository.findByCompanyIdOrderByTimestampDesc(companyId, pageable)
    }

    /**
     * Pobiera historię dla konkretnego typu salda - PROSTA METODA
     */
    @Transactional(readOnly = true)
    fun getBalanceHistoryByType(
        companyId: Long,
        balanceType: BalanceType,
        pageable: Pageable
    ): Page<BalanceHistoryEntity> {
        return balanceHistoryRepository.findByCompanyIdAndBalanceTypeOrderByTimestampDesc(
            companyId, balanceType.name, pageable
        )
    }

    /**
     * Pobiera historię w określonym przedziale czasowym - PROSTA METODA
     */
    @Transactional(readOnly = true)
    fun getBalanceHistoryByDateRange(
        companyId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<BalanceHistoryEntity> {
        return balanceHistoryRepository.findByCompanyIdAndTimestampBetweenOrderByTimestampDesc(
            companyId, startDate, endDate, pageable
        )
    }

    /**
     * Pobiera historię dla konkretnego użytkownika - PROSTA METODA
     */
    @Transactional(readOnly = true)
    fun getBalanceHistoryByUser(
        companyId: Long,
        userId: String,
        pageable: Pageable
    ): Page<BalanceHistoryEntity> {
        return balanceHistoryRepository.findByCompanyIdAndUserIdOrderByTimestampDesc(
            companyId, userId, pageable
        )
    }

    /**
     * Pobiera historię dla konkretnego dokumentu - PROSTA METODA
     */
    @Transactional(readOnly = true)
    fun getBalanceHistoryByDocument(
        companyId: Long,
        documentId: String
    ): List<BalanceHistoryEntity> {
        return balanceHistoryRepository.findByCompanyIdAndDocumentIdOrderByTimestampDesc(
            companyId, documentId
        )
    }

    /**
     * Pobiera ostatnią operację dla danego typu salda - PROSTA METODA
     */
    @Transactional(readOnly = true)
    fun getLastOperationForBalanceType(
        companyId: Long,
        balanceType: BalanceType
    ): BalanceHistoryEntity? {
        return balanceHistoryRepository.findLastOperationForBalanceType(companyId, balanceType.name)
    }

    /**
     * Zlicza operacje w określonym przedziale czasowym - PROSTA METODA
     */
    @Transactional(readOnly = true)
    fun countOperationsInPeriod(
        companyId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long {
        return balanceHistoryRepository.countByCompanyIdAndTimestampBetween(companyId, startDate, endDate)
    }

    /**
     * Pobiera statystyki operacji dla danego typu salda
     */
    @Transactional(readOnly = true)
    fun getBalanceStatistics(
        companyId: Long,
        balanceType: BalanceType,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): BalanceStatistics {
        val operations = balanceHistoryRepository.findByCompanyIdAndBalanceTypeOrderByTimestampDesc(
            companyId, balanceType.name, Pageable.unpaged()
        ).content.filter { it.timestamp >= startDate && it.timestamp <= endDate }

        val totalOperations = operations.size
        val totalAmountChanged = operations.sumOf { it.amountChanged.abs() }
        val positiveChanges = operations.count { it.amountChanged > BigDecimal.ZERO }
        val negativeChanges = operations.count { it.amountChanged < BigDecimal.ZERO }

        val firstOperation = operations.minByOrNull { it.timestamp }
        val lastOperation = operations.maxByOrNull { it.timestamp }

        val startBalance = firstOperation?.balanceBefore ?: BigDecimal.ZERO
        val endBalance = lastOperation?.balanceAfter ?: BigDecimal.ZERO

        return BalanceStatistics(
            totalOperations = totalOperations,
            totalAmountChanged = totalAmountChanged,
            positiveChanges = positiveChanges,
            negativeChanges = negativeChanges,
            startBalance = startBalance,
            endBalance = endBalance,
            netChange = endBalance - startBalance
        )
    }

    // HELPER METHODS

    private fun otherFiltersEmpty(vararg filters: Any?): Boolean {
        return filters.all { it == null }
    }

    private fun convertMapToJson(map: Map<String, Any>): String {
        // Prosta implementacja konwersji do JSON
        return map.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ","
        ) { (key, value) ->
            "\"$key\":\"$value\""
        }
    }
}

/**
 * Statystyki operacji saldowych
 */
data class BalanceStatistics(
    val totalOperations: Int,
    val totalAmountChanged: BigDecimal,
    val positiveChanges: Int,
    val negativeChanges: Int,
    val startBalance: BigDecimal,
    val endBalance: BigDecimal,
    val netChange: BigDecimal
)