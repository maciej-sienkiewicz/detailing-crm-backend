// src/main/kotlin/com/carslab/crm/finances/domain/ports/FixedCostRepository.kt
package com.carslab.crm.finances.domain.ports.fixedcosts

import com.carslab.crm.api.model.FixedCostFilterDTO
import com.carslab.crm.finances.domain.PaginatedResult
import com.carslab.crm.finances.domain.model.fixedcosts.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Port repozytorium dla kosztów stałych
 */
interface FixedCostRepository {

    /**
     * Zapisuje nowy koszt stały lub aktualizuje istniejący
     */
    fun save(fixedCost: FixedCost): FixedCost

    /**
     * Znajduje koszt stały po identyfikatorze
     */
    fun findById(id: FixedCostId): FixedCost?

    /**
     * Znajduje wszystkie koszty stałe z filtrowaniem i paginacją
     */
    fun findAll(filter: FixedCostFilterDTO? = null, page: Int = 0, size: Int = 20): PaginatedResult<FixedCost>

    /**
     * Usuwa koszt stały po identyfikatorze
     */
    fun deleteById(id: FixedCostId): Boolean

    /**
     * Znajduje aktywne koszty stałe w danym okresie
     */
    fun findActiveInPeriod(startDate: LocalDate, endDate: LocalDate): List<FixedCost>

    /**
     * Znajduje koszty stałe według kategorii
     */
    fun findByCategory(category: FixedCostCategory): List<FixedCost>

    /**
     * Znajduje koszty stałe według statusu
     */
    fun findByStatus(status: FixedCostStatus): List<FixedCost>

    /**
     * Oblicza łączne koszty stałe dla danego okresu
     */
    fun calculateTotalFixedCostsForPeriod(startDate: LocalDate, endDate: LocalDate): BigDecimal

    /**
     * Pobiera podsumowanie kosztów według kategorii
     */
    fun getCategorySummary(period: LocalDate): Map<FixedCostCategory, BigDecimal>

    /**
     * Znajduje nadchodzące płatności w określonym przedziale dni
     */
    fun findUpcomingPayments(days: Int): List<UpcomingPayment>

    /**
     * Znajduje przeterminowane płatności
     */
    fun findOverduePayments(): List<OverduePayment>

    /**
     * Zapisuje płatność kosztu stałego
     */
    fun savePayment(fixedCostId: FixedCostId, payment: FixedCostPayment): FixedCostPayment

    /**
     * Pobiera płatności dla kosztu stałego w określonym okresie
     */
    fun getPaymentsForPeriod(fixedCostId: FixedCostId, startDate: LocalDate, endDate: LocalDate): List<FixedCostPayment>

    /**
     * Oblicza statystyki płatności dla kosztu stałego
     */
    fun getPaymentStatistics(fixedCostId: FixedCostId): PaymentStatistics
}

// ============ HELPER DATA CLASSES ============

/**
 * Nadchodząca płatność
 */
data class UpcomingPayment(
    val fixedCostId: FixedCostId,
    val fixedCostName: String,
    val category: FixedCostCategory,
    val dueDate: LocalDate,
    val amount: BigDecimal,
    val supplierName: String?
)

/**
 * Przeterminowana płatność
 */
data class OverduePayment(
    val fixedCostId: FixedCostId,
    val fixedCostName: String,
    val category: FixedCostCategory,
    val dueDate: LocalDate,
    val amount: BigDecimal,
    val daysOverdue: Int,
    val supplierName: String?
)

/**
 * Statystyki płatności
 */
data class PaymentStatistics(
    val totalPaid: BigDecimal,
    val totalPlanned: BigDecimal,
    val averageVariance: BigDecimal,
    val onTimePayments: Int,
    val latePayments: Int,
    val lastPaymentDate: LocalDate?
)
