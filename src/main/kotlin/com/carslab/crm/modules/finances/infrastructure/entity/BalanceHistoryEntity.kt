package com.carslab.crm.modules.finances.infrastructure.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Encja przechowująca pełną historię zmian sald dla celów audytowych i analizy.
 * Każda operacja zmieniająca saldo powoduje utworzenie wpisu w tej tabeli.
 */
@Entity
@Table(name = "balance_history")
class BalanceHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "balance_type", nullable = false, length = 20)
    val balanceType: String, // CASH lub BANK

    @Column(name = "balance_before", nullable = false, precision = 19, scale = 2)
    val balanceBefore: BigDecimal,

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    val balanceAfter: BigDecimal,

    @Column(name = "amount_changed", nullable = false, precision = 19, scale = 2)
    val amountChanged: BigDecimal, // Może być ujemna

    @Column(name = "operation_type", nullable = false, length = 50)
    val operationType: String,

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "document_id")
    val documentId: String?, // ID dokumentu jeśli operacja związana z dokumentem

    @Column(name = "operation_id")
    val operationId: Long?, // ID z balance_operations

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ip_address")
    val ipAddress: String? = null,

    @Column(name = "metadata", columnDefinition = "TEXT")
    val metadata: String? = null // JSON z dodatkowymi informacjami
)