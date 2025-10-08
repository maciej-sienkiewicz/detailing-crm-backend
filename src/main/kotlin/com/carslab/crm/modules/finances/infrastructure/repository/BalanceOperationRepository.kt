package com.carslab.crm.modules.finances.infrastructure.repository

import com.carslab.crm.modules.finances.infrastructure.entity.BalanceOperationEntity
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface BalanceOperationRepository : JpaRepository<BalanceOperationEntity, Long> {

    fun findByCompanyIdAndCreatedAtBetween(
        companyId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<BalanceOperationEntity>

    @Query("SELECT SUM(CASE WHEN o.operationType = 'ADD' THEN o.amount ELSE -o.amount END) " +
            "FROM BalanceOperationEntity o WHERE o.companyId = :companyId AND o.balanceType = :balanceType")
    fun calculateNetBalance(@Param("companyId") companyId: Long, @Param("balanceType") balanceType: BalanceType): BigDecimal?
    
    @Query("SELECT SUM(o.amount) " +
            "FROM BalanceOperationEntity o WHERE o.companyId = :companyId and lower(o.operationType) = lower(:operationType)")
    fun findAmountByCompanyIdAndOperationType(companyId: Long, operationType: String): BigDecimal
}