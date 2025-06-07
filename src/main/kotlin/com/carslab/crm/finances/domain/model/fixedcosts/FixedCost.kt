// src/main/kotlin/com/carslab/crm/finances/domain/model/FixedCost.kt
package com.carslab.crm.finances.domain.model.fixedcosts

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Główny model domenowy kosztu stałego
 */
data class FixedCost(
    val id: FixedCostId,
    val name: String,
    val description: String?,
    val category: FixedCostCategory,
    val monthlyAmount: BigDecimal,
    val frequency: CostFrequency,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: FixedCostStatus,
    val autoRenew: Boolean,
    val supplierInfo: SupplierInfo?,
    val contractNumber: String?,
    val notes: String?,
    val payments: List<FixedCostPayment>,
    val audit: Audit
) {
    /**
     * Oblicza miesięczny koszt niezależnie od częstotliwości
     */
    fun calculateMonthlyAmount(): BigDecimal {
        return when (frequency) {
            CostFrequency.MONTHLY -> monthlyAmount
            CostFrequency.QUARTERLY -> monthlyAmount.multiply(BigDecimal("0.33333"))
            CostFrequency.YEARLY -> monthlyAmount.divide(BigDecimal("12"), 2, BigDecimal.ROUND_HALF_UP)
            CostFrequency.WEEKLY -> monthlyAmount.multiply(BigDecimal("4.33"))
            CostFrequency.ONE_TIME -> BigDecimal.ZERO
        }
    }

    /**
     * Sprawdza czy koszt jest aktywny w danym okresie
     */
    fun isActiveInPeriod(date: LocalDate): Boolean {
        if (status != FixedCostStatus.ACTIVE) return false
        if (date.isBefore(startDate)) return false
        if (endDate != null && date.isAfter(endDate)) return false
        return true
    }

    /**
     * Pobiera płatności za dany miesiąc
     */
    fun getPaymentsForMonth(year: Int, month: Int): List<FixedCostPayment> {
        return payments.filter { payment ->
            payment.paymentDate.year == year && payment.paymentDate.monthValue == month
        }
    }
}

/**
 * Identyfikator kosztu stałego
 */
@JvmInline
value class FixedCostId(val value: String) {
    companion object {
        fun generate(): FixedCostId = FixedCostId(UUID.randomUUID().toString())
    }
}

/**
 * Informacje o dostawcy
 */
data class SupplierInfo(
    val name: String,
    val taxId: String?
)

/**
 * Model płatności kosztu stałego
 */
data class FixedCostPayment(
    val id: String,
    val paymentDate: LocalDate,
    val amount: BigDecimal,
    val plannedAmount: BigDecimal,
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod?,
    val documentId: String?,
    val notes: String?,
    val createdAt: LocalDateTime
)

/**
 * Kategorie kosztów stałych
 */
enum class FixedCostCategory(val displayName: String, val description: String) {
    PERSONNEL("Personel", "Wynagrodzenia, ZUS, benefity pracownicze"),
    LOCATION("Lokalizacja", "Wynajem, czynsz, utrzymanie lokalu"),
    EQUIPMENT("Sprzęt", "Leasing pojazdów, maszyn, narzędzi"),
    MARKETING("Marketing", "Reklama, promocja, strony internetowe"),
    UTILITIES("Media", "Prąd, woda, gaz, internet, telefon"),
    INSURANCE("Ubezpieczenia", "Ubezpieczenia majątkowe, OC, zdrowotne"),
    LICENSES("Licencje", "Oprogramowanie, certyfikaty, pozwolenia"),
    FINANCIAL("Koszty finansowe", "Odsetki, opłaty bankowe, prowizje"),
    OTHER("Inne", "Pozostałe koszty stałe")
}

/**
 * Częstotliwość kosztów
 */
enum class CostFrequency(val displayName: String, val monthsInterval: Int) {
    WEEKLY("Tygodniowo", 0),
    MONTHLY("Miesięcznie", 1),
    QUARTERLY("Kwartalnie", 3),
    YEARLY("Rocznie", 12),
    ONE_TIME("Jednorazowo", 0)
}

/**
 * Status kosztu stałego
 */
enum class FixedCostStatus(val displayName: String) {
    ACTIVE("Aktywny"),
    INACTIVE("Nieaktywny"),
    PAUSED("Wstrzymany"),
    CANCELLED("Anulowany")
}

/**
 * Status płatności
 */
enum class PaymentStatus(val displayName: String) {
    PLANNED("Zaplanowana"),
    PAID("Opłacona"),
    OVERDUE("Przeterminowana"),
    CANCELLED("Anulowana")
}