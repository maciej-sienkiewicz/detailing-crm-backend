// src/main/kotlin/com/carslab/crm/finances/domain/model/BreakevenAnalysis.kt
package com.carslab.crm.finances.domain.model.fixedcosts

import com.carslab.crm.domain.model.Audit
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

/**
 * Konfiguracja do analizy break-even
 */
data class BreakevenConfiguration(
    val id: BreakevenConfigurationId,
    val name: String,
    val description: String?,
    val averageServicePrice: BigDecimal,
    val averageMarginPercentage: BigDecimal,
    val workingDaysPerMonth: Int,
    val targetServicesPerDay: Int?,
    val isActive: Boolean,
    val audit: Audit
) {
    /**
     * Oblicza marżę ze sprzedaży jednej usługi
     */
    fun calculateContributionMargin(): BigDecimal {
        return averageServicePrice.multiply(averageMarginPercentage)
            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
    }

    /**
     * Oblicza koszt zmienny jednej usługi
     */
    fun calculateVariableCost(): BigDecimal {
        return averageServicePrice.subtract(calculateContributionMargin())
    }
}

/**
 * Identyfikator konfiguracji break-even
 */
@JvmInline
value class BreakevenConfigurationId(val value: String) {
    companion object {
        fun generate(): BreakevenConfigurationId = BreakevenConfigurationId(UUID.randomUUID().toString())
    }
}

/**
 * Wynik analizy break-even
 */
data class BreakevenAnalysis(
    val period: LocalDate, // Miesiąc analizy
    val configuration: BreakevenConfiguration,
    val totalFixedCosts: BigDecimal,
    val contributionMarginPerService: BigDecimal,
    val breakevenPointServices: Int,
    val breakevenPointRevenue: BigDecimal,
    val requiredServicesPerDay: Int,
    val workingDaysNeeded: Int,
    val costBreakdown: Map<FixedCostCategory, BigDecimal>,
    val safetyMarginInfo: SafetyMarginInfo
) {
    /**
     * Oblicza ile dni pracy potrzeba aby osiągnąć break-even
     */
    fun calculateWorkingDaysToBreakeven(): Int {
        val servicesPerDay = configuration.targetServicesPerDay ?: 1
        return if (servicesPerDay > 0) {
            (breakevenPointServices + servicesPerDay - 1) / servicesPerDay // ceiling division
        } else {
            configuration.workingDaysPerMonth
        }
    }

    /**
     * Sprawdza czy break-even jest osiągalny w danym miesiącu
     */
    fun isAchievableInMonth(): Boolean {
        return workingDaysNeeded <= configuration.workingDaysPerMonth
    }

    /**
     * Oblicza przychód potrzebny do osiągnięcia określonego zysku
     */
    fun calculateRevenueForProfit(targetProfit: BigDecimal): BigDecimal {
        val servicesNeeded = totalFixedCosts.add(targetProfit)
            .divide(contributionMarginPerService, 0, RoundingMode.CEILING)
        return servicesNeeded.multiply(configuration.averageServicePrice)
    }
}

/**
 * Informacje o marży bezpieczeństwa
 */
data class SafetyMarginInfo(
    val currentMonthlyRevenue: BigDecimal,
    val safetyMarginAmount: BigDecimal,
    val safetyMarginPercentage: BigDecimal,
    val riskLevel: RiskLevel
)

/**
 * Poziomy ryzyka finansowego
 */
enum class RiskLevel(val displayName: String, val color: String) {
    LOW("Niskie ryzyko", "#4CAF50"),      // Zielony
    MEDIUM("Średnie ryzyko", "#FF9800"),   // Pomarańczowy
    HIGH("Wysokie ryzyko", "#F44336"),     // Czerwony
    CRITICAL("Ryzyko krytyczne", "#9C27B0") // Fioletowy
}

/**
 * Projekcja finansowa
 */
data class FinancialProjection(
    val month: LocalDate,
    val fixedCosts: BigDecimal,
    val projectedRevenue: BigDecimal,
    val projectedProfit: BigDecimal,
    val breakevenPoint: Int,
    val actualServices: Int? = null,
    val actualRevenue: BigDecimal? = null,
    val variance: BigDecimal? = null
) {
    /**
     * Sprawdza czy miesiąc był/będzie rentowny
     */
    fun isProfitable(): Boolean {
        val revenue = actualRevenue ?: projectedRevenue
        return revenue > fixedCosts
    }

    /**
     * Oblicza wskaźnik pokrycia kosztów stałych
     */
    fun calculateCoverageRatio(): BigDecimal {
        val revenue = actualRevenue ?: projectedRevenue
        return if (fixedCosts > BigDecimal.ZERO) {
            revenue.divide(fixedCosts, 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }
}

/**
 * Podsumowanie kategorii kosztów
 */
data class CategorySummary(
    val category: FixedCostCategory,
    val totalAmount: BigDecimal,
    val activeCosts: Int,
    val percentage: BigDecimal,
    val trend: TrendDirection,
    val topCosts: List<TopCostItem>
)

/**
 * Element listy największych kosztów w kategorii
 */
data class TopCostItem(
    val name: String,
    val amount: BigDecimal,
    val percentage: BigDecimal
)

/**
 * Kierunek trendu
 */
enum class TrendDirection {
    UP, DOWN, STABLE
}