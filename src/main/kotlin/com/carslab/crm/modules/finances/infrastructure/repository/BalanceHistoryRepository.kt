package com.carslab.crm.modules.finances.infrastructure.repository

import com.carslab.crm.modules.finances.infrastructure.entity.BalanceHistoryEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BalanceHistoryRepository : JpaRepository<BalanceHistoryEntity, Long>, BalanceHistoryCustomRepository {

    /**
     * Znajduje historię sald dla danej firmy z paginacją - PODSTAWOWA METODA
     */
    fun findByCompanyIdOrderByTimestampDesc(
        companyId: Long,
        pageable: Pageable
    ): Page<BalanceHistoryEntity>

    /**
     * Znajduje historię dla konkretnego typu salda - PROSTA METODA
     */
    fun findByCompanyIdAndBalanceTypeOrderByTimestampDesc(
        companyId: Long,
        balanceType: String,
        pageable: Pageable
    ): Page<BalanceHistoryEntity>

    /**
     * Znajduje historię w określonym przedziale czasowym - PROSTA METODA
     */
    fun findByCompanyIdAndTimestampBetweenOrderByTimestampDesc(
        companyId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<BalanceHistoryEntity>

    /**
     * Znajduje historię dla konkretnego użytkownika - PROSTA METODA
     */
    fun findByCompanyIdAndUserIdOrderByTimestampDesc(
        companyId: Long,
        userId: String,
        pageable: Pageable
    ): Page<BalanceHistoryEntity>

    /**
     * Znajduje historię dla konkretnego dokumentu - PROSTA METODA
     */
    fun findByCompanyIdAndDocumentIdOrderByTimestampDesc(
        companyId: Long,
        documentId: String
    ): List<BalanceHistoryEntity>

    /**
     * Znajduje ostatnią operację dla danego typu salda - PROSTA METODA
     */
    @Query("""
        SELECT h FROM BalanceHistoryEntity h 
        WHERE h.companyId = :companyId AND h.balanceType = :balanceType
        ORDER BY h.timestamp DESC
    """)
    fun findLastOperationForBalanceType(
        @Param("companyId") companyId: Long,
        @Param("balanceType") balanceType: String
    ): BalanceHistoryEntity?

    /**
     * Zlicza operacje w określonym przedziale czasowym - PROSTA METODA
     */
    fun countByCompanyIdAndTimestampBetween(
        companyId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long

    /**
     * Znajduje wszystkie operacje danego typu - PROSTA METODA
     */
    fun findByCompanyIdAndOperationTypeOrderByTimestampDesc(
        companyId: Long,
        operationType: String,
        pageable: Pageable
    ): Page<BalanceHistoryEntity>
}