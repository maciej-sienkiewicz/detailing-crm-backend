package com.carslab.crm.finances.infrastructure.repository

import com.carslab.crm.finances.infrastructure.entitiy.InvoiceEntity
import com.carslab.crm.modules.finances.infrastructure.entitiy.CashHistoryBalanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaCashHistoryFlowRepository : JpaRepository<CashHistoryBalanceEntity, Long>