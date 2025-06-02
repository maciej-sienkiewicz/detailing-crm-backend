package com.carslab.crm.finances.infrastructure.entitiy

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "bank_account_balances")
class BackAccountBalanceEntity(
    @Id
    @Column(name = "company_id", nullable = false)
    var companyId: Long,

    @Column(nullable = false)
    var amount: BigDecimal,

    @Column(nullable = false)
    var lastUpdate: String
)