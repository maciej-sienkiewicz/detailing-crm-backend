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

@Repository
interface CashJpaRepository : JpaRepository<CashTransactionEntity, String> {

    fun findByType(type: TransactionType): List<CashTransactionEntity>

    fun findByVisitId(visitId: String): List<CashTransactionEntity>

    fun findByInvoiceId(invoiceId: String): List<CashTransactionEntity>

    fun findByDateBetween(startDate: LocalDate, endDate: LocalDate): List<CashTransactionEntity>

    @Query("""
        SELECT SUM(CASE WHEN c.type = 'INCOME' THEN c.amount ELSE -c.amount END) 
        FROM CashTransactionEntity c
    """)
    fun calculateCurrentBalance(): BigDecimal?

    @Query("""
        SELECT SUM(c.amount) 
        FROM CashTransactionEntity c 
        WHERE c.type = :type AND c.date BETWEEN :startDate AND :endDate
    """)
    fun sumAmountByTypeAndDateRange(
        @Param("type") type: TransactionType,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): BigDecimal?

    @Query("""
        SELECT COUNT(c) 
        FROM CashTransactionEntity c 
        WHERE c.date BETWEEN :startDate AND :endDate
    """)
    fun countTransactionsByDateRange(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): Int

    @Query("""
        SELECT c FROM CashTransactionEntity c 
        WHERE (:type IS NULL OR c.type = :type)
        AND (:description IS NULL OR LOWER(c.description) LIKE LOWER(CONCAT('%', CAST(:description AS string), '%')))
        AND (:dateFrom IS NULL OR c.date >= :dateFrom)
        AND (:dateTo IS NULL OR c.date <= :dateTo)
        AND (:visitId IS NULL OR c.visitId = :visitId)
        AND (:invoiceId IS NULL OR c.invoiceId = :invoiceId)
        AND (:minAmount IS NULL OR c.amount >= :minAmount)
        AND (:maxAmount IS NULL OR c.amount <= :maxAmount)
        ORDER BY c.date DESC, c.createdAt DESC
    """)
    fun searchTransactions(
        @Param("type") type: TransactionType?,
        @Param("description") description: String?,
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("visitId") visitId: String?,
        @Param("invoiceId") invoiceId: String?,
        @Param("minAmount") minAmount: BigDecimal?,
        @Param("maxAmount") maxAmount: BigDecimal?,
        pageable: Pageable
    ): Page<CashTransactionEntity>

    @Query("""
        SELECT COUNT(c) FROM CashTransactionEntity c 
        WHERE (:type IS NULL OR c.type = :type)
        AND (:description IS NULL OR LOWER(c.description) LIKE LOWER(CONCAT('%', CAST(:description AS string), '%')))
        AND (:dateFrom IS NULL OR c.date >= :dateFrom)
        AND (:dateTo IS NULL OR c.date <= :dateTo)
        AND (:visitId IS NULL OR c.visitId = :visitId)
        AND (:invoiceId IS NULL OR c.invoiceId = :invoiceId)
        AND (:minAmount IS NULL OR c.amount >= :minAmount)
        AND (:maxAmount IS NULL OR c.amount <= :maxAmount)
    """)
    fun countSearchTransactions(
        @Param("type") type: TransactionType?,
        @Param("description") description: String?,
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("visitId") visitId: String?,
        @Param("invoiceId") invoiceId: String?,
        @Param("minAmount") minAmount: BigDecimal?,
        @Param("maxAmount") maxAmount: BigDecimal?
    ): Long
}