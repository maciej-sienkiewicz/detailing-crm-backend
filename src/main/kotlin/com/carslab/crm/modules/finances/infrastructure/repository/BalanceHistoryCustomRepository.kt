package com.carslab.crm.modules.finances.infrastructure.repository

import com.carslab.crm.modules.finances.infrastructure.entity.BalanceHistoryEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * Custom repository interface dla zaawansowanych zapytań historii sald
 */
interface BalanceHistoryCustomRepository {

    fun searchWithCriteria(
        companyId: Long,
        balanceType: String?,
        operationType: String?,
        userId: String?,
        documentId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchText: String?,
        pageable: Pageable
    ): Page<BalanceHistoryEntity>
}