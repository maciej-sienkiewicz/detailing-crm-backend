package com.carslab.crm.finances.infrastructure.repository.fixedcosts

import com.carslab.crm.finances.infrastructure.entity.FixedCostPaymentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface FixedCostPaymentJpaRepository : JpaRepository<FixedCostPaymentEntity, String> {

    fun findByFixedCostIdOrderByPaymentDateDesc(fixedCostId: String): List<FixedCostPaymentEntity>

    fun findByFixedCostIdAndPaymentDateBetween(
        fixedCostId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<FixedCostPaymentEntity>

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0) as totalPaid,
               COALESCE(SUM(p.plannedAmount), 0) as totalPlanned,
               COUNT(p) as totalPayments,
               COUNT(CASE WHEN p.status = 'PAID' THEN 1 END) as onTimePayments
        FROM FixedCostPaymentEntity p 
        WHERE p.fixedCost.id = :fixedCostId
    """)
    fun getPaymentStatistics(@Param("fixedCostId") fixedCostId: String): Array<Any>
}