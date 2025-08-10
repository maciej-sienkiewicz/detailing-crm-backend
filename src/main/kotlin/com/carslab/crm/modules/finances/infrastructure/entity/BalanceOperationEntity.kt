package com.carslab.crm.modules.finances.infrastructure.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "balance_operations")
class BalanceOperationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "document_id")
    val documentId: String?, // Nullable dla manualnych operacji

    @Column(name = "operation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val operationType: BalanceOperationType,

    @Column(name = "balance_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val balanceType: BalanceType,

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    val amount: BigDecimal,

    @Column(name = "previous_balance", nullable = false, precision = 19, scale = 2)
    val previousBalance: BigDecimal,

    @Column(name = "new_balance", nullable = false, precision = 19, scale = 2)
    val newBalance: BigDecimal,

    @Column(name = "user_id", nullable = false)
    val userId: String, // Kto wykonał operację

    @Column(name = "user_name", nullable = false)
    val userName: String = "",

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    val description: String, // Opis powodu - teraz wymagany

    @Column(name = "approved_by")
    val approvedBy: String? = null, // Kto zatwierdził (jeśli wymagane)

    @Column(name = "approval_date")
    val approvalDate: LocalDateTime? = null,

    @Column(name = "is_approved", nullable = false)
    val isApproved: Boolean = true, // Domyślnie zatwierdzone

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // Dodatkowe metadane
    @Column(name = "ip_address")
    val ipAddress: String? = null
)

enum class BalanceOperationType(val displayName: String) {
    ADD("Dodanie środków"),
    SUBTRACT("Odjęcie środków"),
    CORRECTION("Korekta salda"),
    MANUAL_OVERRIDE("Manualne nadpisanie"),
    CASH_WITHDRAWAL("Wypłata gotówki"),
    CASH_DEPOSIT("Wpłata gotówki"),
    BANK_RECONCILIATION("Uzgodnienie bankowe"),
    INVENTORY_ADJUSTMENT("Korekta inwentaryzacyjna"),
    CASH_TO_SAFE("Przeniesienie do sejfu"),
    CASH_FROM_SAFE("Pobranie z sejfu")
}

enum class BalanceType(val displayName: String) {
    CASH("Kasa"),
    BANK("Konto bankowe")
}