package com.carslab.crm.finances.infrastructure.repository.fixedcosts

import com.carslab.crm.finances.domain.model.fixedcosts.CostFrequency
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostCategory
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostStatus
import com.carslab.crm.finances.infrastructure.entity.BreakevenConfigurationEntity
import com.carslab.crm.finances.infrastructure.entity.FixedCostEntity
import com.carslab.crm.finances.infrastructure.entity.FixedCostPaymentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Repository
interface FixedCostJpaRepository : JpaRepository<FixedCostEntity, String> {

    @EntityGraph(value = "FixedCost.withPayments", type = EntityGraph.EntityGraphType.FETCH)
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<FixedCostEntity>

    @EntityGraph(value = "FixedCost.withPayments", type = EntityGraph.EntityGraphType.FETCH)
    fun findByCompanyIdAndId(companyId: Long, id: String): Optional<FixedCostEntity>

    fun findByCompanyIdAndCategory(companyId: Long, category: FixedCostCategory): List<FixedCostEntity>

    fun findByCompanyIdAndStatus(companyId: Long, status: FixedCostStatus): List<FixedCostEntity>

    fun findByCompanyIdAndSupplierNameContainingIgnoreCase(companyId: Long, supplierName: String): List<FixedCostEntity>

    fun findByCompanyIdAndContractNumber(companyId: Long, contractNumber: String): List<FixedCostEntity>

    // Kompleksowe wyszukiwanie z filtrami
    @Query("""
        SELECT DISTINCT e FROM FixedCostEntity e 
        LEFT JOIN FETCH e.payments
        WHERE (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
        AND (:category IS NULL OR e.category = :category)
        AND (:status IS NULL OR e.status = :status)
        AND (:frequency IS NULL OR e.frequency = :frequency)
        AND (:supplierName IS NULL OR LOWER(e.supplierName) LIKE LOWER(CONCAT('%', CAST(:supplierName AS string), '%')))
        AND (:contractNumber IS NULL OR e.contractNumber = :contractNumber)
        AND (:startDateFrom IS NULL OR e.startDate >= :startDateFrom)
        AND (:startDateTo IS NULL OR e.startDate <= :startDateTo)
        AND (:endDateFrom IS NULL OR e.endDate >= :endDateFrom)
        AND (:endDateTo IS NULL OR e.endDate <= :endDateTo)
        AND (:minAmount IS NULL OR e.monthlyAmount >= :minAmount)
        AND (:maxAmount IS NULL OR e.monthlyAmount <= :maxAmount)
        AND e.companyId = :companyId
        ORDER BY e.name ASC
    """)
    fun searchFixedCosts(
        @Param("name") name: String?,
        @Param("category") category: FixedCostCategory?,
        @Param("status") status: FixedCostStatus?,
        @Param("frequency") frequency: CostFrequency?,
        @Param("supplierName") supplierName: String?,
        @Param("contractNumber") contractNumber: String?,
        @Param("startDateFrom") startDateFrom: LocalDate?,
        @Param("startDateTo") startDateTo: LocalDate?,
        @Param("endDateFrom") endDateFrom: LocalDate?,
        @Param("endDateTo") endDateTo: LocalDate?,
        @Param("minAmount") minAmount: BigDecimal?,
        @Param("maxAmount") maxAmount: BigDecimal?,
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<FixedCostEntity>

    // Aktywne koszty w okresie
    @Query("""
        SELECT e FROM FixedCostEntity e 
        WHERE e.status = 'ACTIVE'
        AND e.startDate <= :endDate
        AND (e.endDate IS NULL OR e.endDate >= :startDate)
        AND e.companyId = :companyId
        ORDER BY e.category, e.name
    """)
    fun findActiveInPeriod(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("companyId") companyId: Long
    ): List<FixedCostEntity>

    // Podsumowanie według kategorii
    @Query("""
        SELECT e.category as category, COALESCE(SUM(e.monthlyAmount), 0) as total
        FROM FixedCostEntity e 
        WHERE e.status = 'ACTIVE'
        AND e.startDate <= :periodEnd
        AND (e.endDate IS NULL OR e.endDate >= :periodStart)
        AND e.companyId = :companyId
        GROUP BY e.category
        ORDER BY total DESC
    """)
    fun getCategorySummary(
        @Param("periodStart") periodStart: LocalDate,
        @Param("periodEnd") periodEnd: LocalDate,
        @Param("companyId") companyId: Long
    ): List<Array<Any>>

    // Łączne koszty stałe dla okresu
    @Query("""
        SELECT COALESCE(SUM(e.monthlyAmount), 0)
        FROM FixedCostEntity e 
        WHERE e.status = 'ACTIVE'
        AND e.startDate <= :endDate
        AND (e.endDate IS NULL OR e.endDate >= :startDate)
        AND e.companyId = :companyId
    """)
    fun calculateTotalFixedCostsForPeriod(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("companyId") companyId: Long
    ): BigDecimal?

    // Nadchodzące płatności (symulowane na podstawie częstotliwości)
    @Query("""
        SELECT e.id, e.name, e.category, e.monthlyAmount, e.supplierName, e.frequency
        FROM FixedCostEntity e 
        WHERE e.status = 'ACTIVE'
        AND e.companyId = :companyId
        ORDER BY e.name
    """)
    fun findCostsForUpcomingPayments(@Param("companyId") companyId: Long): List<Array<Any>>
}