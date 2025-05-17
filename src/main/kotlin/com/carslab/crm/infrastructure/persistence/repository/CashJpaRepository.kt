package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.domain.model.view.finance.TransactionType
import com.carslab.crm.infrastructure.persistence.entity.CashTransactionEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@Repository
interface CashJpaRepository : JpaRepository<CashTransactionEntity, String> {
    fun findByCompanyId(companyId: Long): List<CashTransactionEntity>
    fun findByCompanyIdAndId(companyId: Long, id: String): Optional<CashTransactionEntity>
    fun findByTypeAndCompanyId(type: TransactionType, companyId: Long): List<CashTransactionEntity>
    fun findByVisitIdAndCompanyId(visitId: String, companyId: Long): List<CashTransactionEntity>
    fun findByDateBetweenAndCompanyId(startDate: LocalDate, endDate: LocalDate, companyId: Long): List<CashTransactionEntity>

    @Query("""
        SELECT SUM(CASE WHEN c.type = 'INCOME' THEN c.amount ELSE -c.amount END) 
        FROM CashTransactionEntity c
        WHERE c.companyId = :companyId
    """)
    fun calculateCurrentBalanceForCompany(@Param("companyId") companyId: Long): BigDecimal?

    @Query("""
        SELECT SUM(c.amount) 
        FROM CashTransactionEntity c 
        WHERE c.type = :type AND c.date BETWEEN :startDate AND :endDate AND c.companyId = :companyId
    """)
    fun sumAmountByTypeAndDateRangeAndCompanyId(
        @Param("type") type: TransactionType,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("companyId") companyId: Long
    ): BigDecimal?

    @Query("""
        SELECT COUNT(c) 
        FROM CashTransactionEntity c 
        WHERE c.date BETWEEN :startDate AND :endDate AND c.companyId = :companyId
    """)
    fun countTransactionsByDateRangeAndCompanyId(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("companyId") companyId: Long
    ): Int

    @Query("""
        SELECT c FROM CashTransactionEntity c 
        WHERE (:type IS NULL OR c.type = :type)
        AND (:description IS NULL OR LOWER(c.description) LIKE LOWER(CONCAT('%', CAST(:description AS string), '%')))
        AND (:dateFrom IS NULL OR c.date >= :dateFrom)
        AND (:dateTo IS NULL OR c.date <= :dateTo)
        AND (:visitId IS NULL OR c.visitId = :visitId)
        AND (:minAmount IS NULL OR c.amount >= :minAmount)
        AND (:maxAmount IS NULL OR c.amount <= :maxAmount)
        AND c.companyId = :companyId
        ORDER BY c.date DESC, c.createdAt DESC
    """)
    fun searchTransactionsForCompany(
        @Param("type") type: TransactionType?,
        @Param("description") description: String?,
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("visitId") visitId: String?,
        @Param("minAmount") minAmount: BigDecimal?,
        @Param("maxAmount") maxAmount: BigDecimal?,
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<CashTransactionEntity>

    @Query("""
        SELECT COUNT(c) FROM CashTransactionEntity c 
        WHERE (:type IS NULL OR c.type = :type)
        AND (:description IS NULL OR LOWER(c.description) LIKE LOWER(CONCAT('%', CAST(:description AS string), '%')))
        AND (:dateFrom IS NULL OR c.date >= :dateFrom)
        AND (:dateTo IS NULL OR c.date <= :dateTo)
        AND (:visitId IS NULL OR c.visitId = :visitId)
        AND (:minAmount IS NULL OR c.amount >= :minAmount)
        AND (:maxAmount IS NULL OR c.amount <= :maxAmount)
        AND c.companyId = :companyId
    """)
    fun countSearchTransactionsForCompany(
        @Param("type") type: TransactionType?,
        @Param("description") description: String?,
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("visitId") visitId: String?,
        @Param("minAmount") minAmount: BigDecimal?,
        @Param("maxAmount") maxAmount: BigDecimal?,
        @Param("companyId") companyId: Long
    ): Long
}