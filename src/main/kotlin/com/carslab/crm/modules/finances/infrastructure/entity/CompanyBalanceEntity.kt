package com.carslab.crm.modules.finances.infrastructure.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "company_balances")
class CompanyBalanceEntity(
    @Id
    @Column(name = "company_id")
    val companyId: Long,

    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 2)
    var cashBalance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "bank_balance", nullable = false, precision = 19, scale = 2)
    var bankBalance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "last_updated", nullable = false)
    var lastUpdated: LocalDateTime = LocalDateTime.now(),

    @Version // Optimistic locking for concurrent updates
    var version: Long = 0
)