package com.carslab.crm.finances.infrastructure.repository

import com.carslab.crm.domain.model.view.finance.InvoiceStatus
import com.carslab.crm.domain.model.view.finance.InvoiceType
import com.carslab.crm.finances.infrastructure.entitiy.InvoiceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@Repository
interface InvoiceJpaRepository : JpaRepository<InvoiceEntity, String> {
    fun findByCompanyId(companyId: Long): List<InvoiceEntity>
    fun findByCompanyIdAndId(companyId: Long, id: String): Optional<InvoiceEntity>
    fun findByNumberAndCompanyId(number: String, companyId: Long): InvoiceEntity?
    fun findByStatusAndCompanyId(status: InvoiceStatus, companyId: Long): List<InvoiceEntity>
    fun findByTypeAndCompanyId(type: InvoiceType, companyId: Long): List<InvoiceEntity>
    fun findByClientIdAndCompanyId(clientId: Long, companyId: Long): List<InvoiceEntity>
    fun findByProtocolIdAndCompanyId(protocolId: String, companyId: Long): List<InvoiceEntity>
    fun findByDueDateBeforeAndCompanyId(date: LocalDate, companyId: Long): List<InvoiceEntity>
    fun findByBuyerNameContainingIgnoreCaseAndCompanyId(buyerName: String, companyId: Long): List<InvoiceEntity>
    fun findByTitleContainingIgnoreCaseAndCompanyId(title: String, companyId: Long): List<InvoiceEntity>

    @Query("SELECT MAX(e.number) FROM InvoiceEntity e WHERE e.number LIKE CONCAT(:prefix, '%') AND e.companyId = :companyId")
    fun findLastNumberByPrefixAndCompanyId(@Param("prefix") prefix: String, @Param("companyId") companyId: Long): String?

    @Modifying
    @Query("UPDATE InvoiceEntity e SET e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id AND e.companyId = :companyId")
    fun updateStatusAndCompanyId(@Param("id") id: String, @Param("status") status: InvoiceStatus, @Param("companyId") companyId: Long): Int

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
        AND e.companyId = :companyId
        ORDER BY e.issuedDate DESC
    """)
    fun searchInvoicesAndCompanyId(
        @Param("number") number: String?,
        @Param("title") title: String?,
        @Param("buyerName") buyerName: String?,
        @Param("status") status: InvoiceStatus?,
        @Param("type") type: InvoiceType?,
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("protocolId") protocolId: String?,
        @Param("minAmount") minAmount: BigDecimal?,
        @Param("maxAmount") maxAmount: BigDecimal?,
        @Param("companyId") companyId: Long
    ): List<InvoiceEntity>
}