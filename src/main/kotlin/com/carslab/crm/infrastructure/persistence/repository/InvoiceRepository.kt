package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.domain.model.view.finance.InvoiceStatus
import com.carslab.crm.domain.model.view.finance.InvoiceType
import com.carslab.crm.infrastructure.persistence.entity.InvoiceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface InvoiceJpaRepository : JpaRepository<InvoiceEntity, String> {
    fun findByNumber(number: String): InvoiceEntity?

    fun findByStatus(status: InvoiceStatus): List<InvoiceEntity>

    fun findByType(type: InvoiceType): List<InvoiceEntity>

    fun findByClientId(clientId: Long): List<InvoiceEntity>

    fun findByProtocolId(protocolId: String): List<InvoiceEntity>

    fun findByDueDateBefore(date: LocalDate): List<InvoiceEntity>

    fun findByBuyerNameContainingIgnoreCase(buyerName: String): List<InvoiceEntity>

    fun findByTitleContainingIgnoreCase(title: String): List<InvoiceEntity>

    @Query("SELECT MAX(e.number) FROM InvoiceEntity e WHERE e.number LIKE CONCAT(:prefix, '%')")
    fun findLastNumberByPrefix(@Param("prefix") prefix: String): String?

    @Modifying
    @Query("UPDATE InvoiceEntity e SET e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    fun updateStatus(@Param("id") id: String, @Param("status") status: InvoiceStatus): Int

    @Query("""
        SELECT e FROM InvoiceEntity e 
        WHERE (:number IS NULL OR e.number LIKE CONCAT('%', CAST(:number AS string), '%'))
        AND (:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
        AND (:buyerName IS NULL OR LOWER(e.buyerName) LIKE LOWER(CONCAT('%', CAST(:buyerName AS string), '%')))
        AND (:status IS NULL OR e.status = :status)
        AND (:type IS NULL OR e.type = :type)
        AND (:dateFrom IS NULL OR e.issuedDate >= :dateFrom)
        AND (:dateTo IS NULL OR e.issuedDate <= :dateTo)
        AND (:protocolId IS NULL OR e.protocolId = :protocolId)
        AND (:minAmount IS NULL OR e.totalGross >= :minAmount)
        AND (:maxAmount IS NULL OR e.totalGross <= :maxAmount)
        ORDER BY e.issuedDate DESC
    """)
    fun searchInvoices(
        @Param("number") number: String?,
        @Param("title") title: String?,
        @Param("buyerName") buyerName: String?,
        @Param("status") status: InvoiceStatus?,
        @Param("type") type: InvoiceType?,
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("protocolId") protocolId: String?,
        @Param("minAmount") minAmount: BigDecimal?,
        @Param("maxAmount") maxAmount: BigDecimal?
    ): List<InvoiceEntity>
}