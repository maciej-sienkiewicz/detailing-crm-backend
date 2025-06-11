package com.carslab.crm.finances.infrastructure.repository

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.DocumentType
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.infrastructure.entitiy.UnifiedDocumentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@Repository
interface UnifiedDocumentJpaRepository : JpaRepository<UnifiedDocumentEntity, String> {

    // Podstawowe zapytania z Entity Graph dla optymalnego ładowania
    @EntityGraph(value = "UnifiedDocument.withItemsAndAttachment", type = EntityGraph.EntityGraphType.FETCH)
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItemsAndAttachment", type = EntityGraph.EntityGraphType.FETCH)
    fun findByCompanyIdAndId(companyId: Long, id: String): Optional<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByNumberAndCompanyId(number: String, companyId: Long): UnifiedDocumentEntity?

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByStatusAndCompanyId(status: DocumentStatus, companyId: Long): List<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByTypeAndCompanyId(type: DocumentType, companyId: Long): List<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByDirectionAndCompanyId(direction: TransactionDirection, companyId: Long): List<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByTypeAndDirectionAndCompanyId(type: DocumentType, direction: TransactionDirection, companyId: Long): List<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByProtocolIdAndCompanyId(protocolId: String, companyId: Long): List<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByVisitIdAndCompanyId(visitId: String, companyId: Long): List<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByDueDateBeforeAndCompanyId(date: LocalDate, companyId: Long): List<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByBuyerNameContainingIgnoreCaseAndCompanyId(buyerName: String, companyId: Long): List<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findBySellerNameContainingIgnoreCaseAndCompanyId(sellerName: String, companyId: Long): List<UnifiedDocumentEntity>

    @EntityGraph(value = "UnifiedDocument.withItems", type = EntityGraph.EntityGraphType.FETCH)
    fun findByTitleContainingIgnoreCaseAndCompanyId(title: String, companyId: Long): List<UnifiedDocumentEntity>

    // Generowanie numerów dokumentów
    @Query("SELECT MAX(e.number) FROM UnifiedDocumentEntity e WHERE e.number LIKE CONCAT(:prefix, '%') AND e.companyId = :companyId")
    fun findLastNumberByPrefixAndCompanyId(@Param("prefix") prefix: String, @Param("companyId") companyId: Long): String?

    // Aktualizacje
    @Modifying
    @Query("UPDATE UnifiedDocumentEntity e SET e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id AND e.companyId = :companyId")
    fun updateStatusAndCompanyId(@Param("id") id: String, @Param("status") status: DocumentStatus, @Param("companyId") companyId: Long): Int

    @Modifying
    @Query("UPDATE UnifiedDocumentEntity e SET e.paidAmount = :paidAmount, e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id AND e.companyId = :companyId")
    fun updatePaidAmountAndStatusAndCompanyId(
        @Param("id") id: String,
        @Param("paidAmount") paidAmount: BigDecimal,
        @Param("status") status: DocumentStatus,
        @Param("companyId") companyId: Long
    ): Int

    // Kompleksowe wyszukiwanie z filtrami - ZOPTYMALIZOWANE
    @Query("""
        SELECT DISTINCT e FROM UnifiedDocumentEntity e 
        LEFT JOIN FETCH e.items
        LEFT JOIN FETCH e.attachment
        WHERE (:number IS NULL OR e.number LIKE CONCAT('%', CAST(:number AS string), '%'))
        AND (:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
        AND (:buyerName IS NULL OR LOWER(e.buyerName) LIKE LOWER(CONCAT('%', CAST(:buyerName AS string), '%')))
        AND (:sellerName IS NULL OR LOWER(e.sellerName) LIKE LOWER(CONCAT('%', CAST(:sellerName AS string), '%')))
        AND (:status IS NULL OR e.status = :status)
        AND (:type IS NULL OR e.type = :type)
        AND (:direction IS NULL OR e.direction = :direction)
        AND (:paymentMethod IS NULL OR e.paymentMethod = :paymentMethod)
        AND (:dateFrom IS NULL OR e.issuedDate >= :dateFrom)
        AND (:dateTo IS NULL OR e.issuedDate <= :dateTo)
        AND (:protocolId IS NULL OR e.protocolId = :protocolId)
        AND (:visitId IS NULL OR e.visitId = :visitId)
        AND (:minAmount IS NULL OR e.totalGross >= :minAmount)
        AND (:maxAmount IS NULL OR e.totalGross <= :maxAmount)
        AND e.companyId = :companyId
        ORDER BY e.issuedDate DESC
    """)
    fun searchDocumentsAndCompanyId(
        @Param("number") number: String?,
        @Param("title") title: String?,
        @Param("buyerName") buyerName: String?,
        @Param("sellerName") sellerName: String?,
        @Param("status") status: DocumentStatus?,
        @Param("type") type: DocumentType?,
        @Param("direction") direction: TransactionDirection?,
        @Param("paymentMethod") paymentMethod: PaymentMethod?,
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("protocolId") protocolId: String?,
        @Param("visitId") visitId: String?,
        @Param("minAmount") minAmount: BigDecimal?,
        @Param("maxAmount") maxAmount: BigDecimal?,
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<UnifiedDocumentEntity>

    // Zapytania finansowe i statystyczne - zoptymalizowane bez JOIN FETCH
    @Query("""
        SELECT COALESCE(SUM(e.totalGross), 0) FROM UnifiedDocumentEntity e 
        WHERE (:dateFrom IS NULL OR e.issuedDate >= :dateFrom)
        AND (:dateTo IS NULL OR e.issuedDate <= :dateTo)
        AND (:direction IS NULL OR e.direction = :direction)
        AND (:status IS NULL OR e.status = :status)
        AND e.companyId = :companyId
    """)
    fun calculateTotalForPeriodAndCompanyId(
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("direction") direction: TransactionDirection?,
        @Param("status") status: DocumentStatus?,
        @Param("companyId") companyId: Long
    ): BigDecimal

    @Query("""
        SELECT COALESCE(SUM(e.paidAmount), 0) FROM UnifiedDocumentEntity e 
        WHERE (:dateFrom IS NULL OR e.issuedDate >= :dateFrom)
        AND (:dateTo IS NULL OR e.issuedDate <= :dateTo)
        AND e.companyId = :companyId
    """)
    fun calculateTotalPaidForPeriodAndCompanyId(
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("companyId") companyId: Long
    ): BigDecimal

    @Query("""
        SELECT COUNT(e) FROM UnifiedDocumentEntity e 
        WHERE (:dateFrom IS NULL OR e.issuedDate >= :dateFrom)
        AND (:dateTo IS NULL OR e.issuedDate <= :dateTo)
        AND e.companyId = :companyId
    """)
    fun countDocumentsForPeriodAndCompanyId(
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("companyId") companyId: Long
    ): Long

    @Query("""
        SELECT COUNT(e) FROM UnifiedDocumentEntity e 
        WHERE e.dueDate < :date 
        AND e.status IN ('NOT_PAID', 'PARTIALLY_PAID')
        AND e.companyId = :companyId
    """)
    fun countOverdueDocumentsAndCompanyId(@Param("date") date: LocalDate, @Param("companyId") companyId: Long): Long

    @Query("""
        SELECT COALESCE(SUM(e.totalGross - e.paidAmount), 0) FROM UnifiedDocumentEntity e 
        WHERE e.dueDate < :date 
        AND e.status IN ('NOT_PAID', 'PARTIALLY_PAID', 'OVERDUE')
        AND e.companyId = :companyId
    """)
    fun calculateOverdueStatusAndCompanyId(@Param("date") date: LocalDate, @Param("companyId") companyId: Long): BigDecimal

    // Statystyki metod płatności
    @Query("""
        SELECT e.paymentMethod as method, COUNT(e) as count, COALESCE(SUM(e.totalGross), 0) as total
        FROM UnifiedDocumentEntity e 
        WHERE e.companyId = :companyId
        GROUP BY e.paymentMethod 
        ORDER BY count DESC
    """)
    fun getPaymentMethodStatsAndCompanyId(@Param("companyId") companyId: Long): List<Array<Any>>

    // Statystyki kontrahentów
    @Query("""
        SELECT e.buyerName as name, e.buyerTaxId as taxId, COUNT(e) as count, COALESCE(SUM(e.totalGross), 0) as total
        FROM UnifiedDocumentEntity e 
        WHERE e.companyId = :companyId
        GROUP BY e.buyerName, e.buyerTaxId 
        ORDER BY count DESC
    """)
    fun getBuyerStatsAndCompanyId(@Param("companyId") companyId: Long, pageable: Pageable): List<Array<Any>>

    @Query("""
        SELECT e.sellerName as name, e.sellerTaxId as taxId, COUNT(e) as count, COALESCE(SUM(e.totalGross), 0) as total
        FROM UnifiedDocumentEntity e 
        WHERE e.companyId = :companyId
        GROUP BY e.sellerName, e.sellerTaxId 
        ORDER BY count DESC
    """)
    fun getSellerStatsAndCompanyId(@Param("companyId") companyId: Long, pageable: Pageable): List<Array<Any>>

    // Dane do wykresów - miesięczne przychody/wydatki
    @Query("""
        SELECT 
        YEAR(e.issuedDate) as year,
        MONTH(e.issuedDate) as month,
        e.direction as direction,
        COALESCE(SUM(e.totalGross), 0) as total
        FROM UnifiedDocumentEntity e 
        WHERE e.issuedDate >= :dateFrom 
        AND e.issuedDate <= :dateTo
        AND e.companyId = :companyId
        GROUP BY YEAR(e.issuedDate), MONTH(e.issuedDate), e.direction
        ORDER BY year, month
    """)
    fun getMonthlyChartDataAndCompanyId(
        @Param("dateFrom") dateFrom: LocalDate,
        @Param("dateTo") dateTo: LocalDate,
        @Param("companyId") companyId: Long
    ): List<Array<Any>>

    // Dane do wykresów - tygodniowe przychody/wydatki
    @Query("""
        SELECT 
        YEAR(e.issuedDate) as year,
        WEEK(e.issuedDate) as week,
        e.direction as direction,
        COALESCE(SUM(e.totalGross), 0) as total
        FROM UnifiedDocumentEntity e 
        WHERE e.issuedDate >= :dateFrom 
        AND e.issuedDate <= :dateTo
        AND e.companyId = :companyId
        GROUP BY YEAR(e.issuedDate), WEEK(e.issuedDate), e.direction
        ORDER BY year, week
    """)
    fun getWeeklyChartDataAndCompanyId(
        @Param("dateFrom") dateFrom: LocalDate,
        @Param("dateTo") dateTo: LocalDate,
        @Param("companyId") companyId: Long
    ): List<Array<Any>>

    // Dane do wykresów - dzienne przychody/wydatki
    @Query("""
        SELECT 
        e.issuedDate as date,
        e.direction as direction,
        COALESCE(SUM(e.totalGross), 0) as total
        FROM UnifiedDocumentEntity e 
        WHERE e.issuedDate >= :dateFrom 
        AND e.issuedDate <= :dateTo
        AND e.companyId = :companyId
        GROUP BY e.issuedDate, e.direction
        ORDER BY e.issuedDate
    """)
    fun getDailyChartDataAndCompanyId(
        @Param("dateFrom") dateFrom: LocalDate,
        @Param("dateTo") dateTo: LocalDate,
        @Param("companyId") companyId: Long
    ): List<Array<Any>>

    @EntityGraph(value = "UnifiedDocument.withItemsAndAttachment", type = EntityGraph.EntityGraphType.FETCH)
    @Query("""
    SELECT e FROM UnifiedDocumentEntity e 
    WHERE e.companyId = :companyId
    AND e.type = 'INVOICE'
    AND e.issuedDate >= :startDate
    AND e.issuedDate <= :endDate
    ORDER BY e.issuedDate DESC
""")
    fun findInvoicesByCompanyAndDateRange(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<UnifiedDocumentEntity>
}