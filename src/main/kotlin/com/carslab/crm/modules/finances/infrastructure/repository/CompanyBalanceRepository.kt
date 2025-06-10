package com.carslab.crm.modules.finances.infrastructure.repository

import com.carslab.crm.modules.finances.infrastructure.entity.CompanyBalanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface CompanyBalanceRepository : JpaRepository<CompanyBalanceEntity, Long> {

    @Query("""
        SELECT 
            CASE 
                WHEN d.paymentMethod = 'CASH' THEN 'cash'
                ELSE 'bank' 
            END as balanceType,
            SUM(CASE 
                WHEN d.direction = 'INCOME' AND d.status = 'PAID' THEN d.totalGross 
                WHEN d.direction = 'EXPENSE' AND d.status = 'PAID' THEN -d.totalGross 
                ELSE 0 
            END) as balance
        FROM UnifiedDocumentEntity d 
        WHERE d.companyId = :companyId
        GROUP BY CASE 
            WHEN d.paymentMethod = 'CASH' THEN 'cash'
            ELSE 'bank' 
        END
    """)
    fun calculateBalancesFromTransactions(@Param("companyId") companyId: Long): Map<String, BigDecimal>
}