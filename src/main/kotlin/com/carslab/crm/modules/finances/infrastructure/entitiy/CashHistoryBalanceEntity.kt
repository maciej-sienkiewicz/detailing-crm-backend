package com.carslab.crm.modules.finances.infrastructure.entitiy

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "cash_balances_flow")
class CashHistoryBalanceEntity(
    @Id
    var id: Long,

    @Column(name = "company_id", nullable = false)
    var companyId: Long,

    @Column(nullable = false)
    var previousAmount: BigDecimal,

    @Column(nullable = false)
    var afterOperation: BigDecimal,

    @Column(nullable = false)
    var lastUpdate: String
)