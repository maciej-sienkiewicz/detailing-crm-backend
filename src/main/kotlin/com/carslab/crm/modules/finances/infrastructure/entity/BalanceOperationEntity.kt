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

    // 🔥 NOWE POLA dla manualnych operacji
    @Column(name = "override_reason")
    @Enumerated(EnumType.STRING)
    val overrideReason: OverrideReason? = null,

    @Column(name = "user_id", nullable = false)
    val userId: String, // Kto wykonał operację

    @Column(name = "user_name", nullable = false)
    val userName: String, // Imię i nazwisko dla audytu

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null, // Opis powodu

    @Column(name = "approved_by")
    val approvedBy: String? = null, // Kto zatwierdził (jeśli wymagane)

    @Column(name = "approval_date")
    val approvalDate: LocalDateTime? = null,

    @Column(name = "is_approved", nullable = false)
    val isApproved: Boolean = true, // Domyślnie zatwierdzone, chyba że wymaga zatwierdzenia

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // Dodatkowe metadane
    @Column(name = "ip_address")
    val ipAddress: String? = null,

    @Column(name = "user_agent")
    val userAgent: String? = null
)

enum class BalanceOperationType {
    ADD,
    SUBTRACT,
    CORRECTION,
    MANUAL_OVERRIDE,
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BANK_RECONCILIATION,
    INVENTORY_ADJUSTMENT
}

enum class OverrideReason(val displayName: String, val requiresApproval: Boolean) {
    CASH_TO_SAFE("Przeniesienie gotówki do sejfu", false),
    CASH_FROM_SAFE("Pobranie gotówki z sejfu", false),
    BANK_STATEMENT_RECONCILIATION("Uzgodnienie z wyciągiem bankowym", true),
    INVENTORY_COUNT("Rezultat inwentaryzacji kasy", true),
    ERROR_CORRECTION("Korekta błędu księgowego", true),
    EXTERNAL_PAYMENT("Płatność zewnętrzna nie odnotowana w systemie", true),
    MANAGER_ADJUSTMENT("Korekta menedżerska", true),
    SYSTEM_MIGRATION("Migracja danych systemowych", true),
    OTHER("Inna przyczyna", true)
}

enum class BalanceType { CASH, BANK }

