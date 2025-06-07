// src/main/kotlin/com/carslab/crm/api/model/FixedCostDTOs.kt
package com.carslab.crm.api.model

import com.carslab.crm.finances.domain.model.fixedcosts.CostFrequency
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostCategory
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostStatus
import com.carslab.crm.finances.domain.model.fixedcosts.PaymentStatus
import com.carslab.crm.finances.domain.model.fixedcosts.RiskLevel
import com.carslab.crm.finances.domain.model.fixedcosts.TrendDirection
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

// ============ REQUEST DTOs ============

/**
 * Request do tworzenia nowego kosztu stałego
 */
data class CreateFixedCostRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @field:NotNull(message = "Category is required")
    val category: FixedCostCategory,

    @field:NotNull(message = "Monthly amount is required")
    @field:DecimalMin(value = "0.01", message = "Monthly amount must be greater than 0")
    @field:Digits(integer = 17, fraction = 2, message = "Invalid amount format")
    val monthlyAmount: BigDecimal,

    @field:NotNull(message = "Frequency is required")
    val frequency: CostFrequency,

    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate,

    val endDate: LocalDate? = null,

    @field:NotNull(message = "Status is required")
    val status: FixedCostStatus = FixedCostStatus.ACTIVE,

    val autoRenew: Boolean = false,

    @field:Valid
    val supplierInfo: SupplierInfoDTO? = null,

    @field:Size(max = 100, message = "Contract number must not exceed 100 characters")
    val contractNumber: String? = null,

    @field:Size(max = 2000, message = "Notes must not exceed 2000 characters")
    val notes: String? = null
) {
    init {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw IllegalArgumentException("End date cannot be before start date")
        }
    }
}

/**
 * Request do aktualizacji kosztu stałego
 */
data class UpdateFixedCostRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @field:NotNull(message = "Category is required")
    val category: FixedCostCategory,

    @field:NotNull(message = "Monthly amount is required")
    @field:DecimalMin(value = "0.01", message = "Monthly amount must be greater than 0")
    @field:Digits(integer = 17, fraction = 2, message = "Invalid amount format")
    val monthlyAmount: BigDecimal,

    @field:NotNull(message = "Frequency is required")
    val frequency: CostFrequency,

    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate,

    val endDate: LocalDate? = null,

    @field:NotNull(message = "Status is required")
    val status: FixedCostStatus,

    val autoRenew: Boolean = false,

    @field:Valid
    val supplierInfo: SupplierInfoDTO? = null,

    @field:Size(max = 100, message = "Contract number must not exceed 100 characters")
    val contractNumber: String? = null,

    @field:Size(max = 2000, message = "Notes must not exceed 2000 characters")
    val notes: String? = null
) {
    init {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw IllegalArgumentException("End date cannot be before start date")
        }
    }
}

/**
 * Request do rejestracji płatności
 */
data class RecordPaymentRequest(
    @field:NotNull(message = "Payment date is required")
    val paymentDate: LocalDate,

    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @field:Digits(integer = 17, fraction = 2, message = "Invalid amount format")
    val amount: BigDecimal,

    @field:NotNull(message = "Planned amount is required")
    @field:DecimalMin(value = "0.01", message = "Planned amount must be greater than 0")
    @field:Digits(integer = 17, fraction = 2, message = "Invalid planned amount format")
    val plannedAmount: BigDecimal,

    @field:NotNull(message = "Status is required")
    val status: PaymentStatus,

    val paymentMethod: String? = null, // PaymentMethod enum as string

    val documentId: String? = null,

    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null
)

/**
 * Request do konfiguracji break-even
 */
data class BreakevenConfigurationRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @field:NotNull(message = "Average service price is required")
    @field:DecimalMin(value = "0.01", message = "Average service price must be greater than 0")
    @field:Digits(integer = 17, fraction = 2, message = "Invalid price format")
    val averageServicePrice: BigDecimal,

    @field:NotNull(message = "Average margin percentage is required")
    @field:DecimalMin(value = "0.01", message = "Margin percentage must be greater than 0")
    @field:DecimalMax(value = "100.00", message = "Margin percentage cannot exceed 100%")
    @field:Digits(integer = 3, fraction = 2, message = "Invalid percentage format")
    val averageMarginPercentage: BigDecimal,

    @field:NotNull(message = "Working days per month is required")
    @field:Min(value = 1, message = "Working days per month must be at least 1")
    @field:Max(value = 31, message = "Working days per month cannot exceed 31")
    val workingDaysPerMonth: Int = 22,

    @field:Min(value = 1, message = "Target services per day must be at least 1")
    val targetServicesPerDay: Int? = null,

    val isActive: Boolean = true
)

// ============ RESPONSE DTOs ============

/**
 * Response DTO dla kosztu stałego
 */
data class FixedCostResponse(
    val id: String,
    val name: String,
    val description: String?,
    val category: FixedCostCategory,
    val categoryDisplay: String,
    val monthlyAmount: BigDecimal,
    val frequency: CostFrequency,
    val frequencyDisplay: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: FixedCostStatus,
    val statusDisplay: String,
    val autoRenew: Boolean,
    val supplierInfo: SupplierInfoDTO?,
    val contractNumber: String?,
    val notes: String?,
    val calculatedMonthlyAmount: BigDecimal,
    val totalPaid: BigDecimal,
    val totalPlanned: BigDecimal,
    val lastPaymentDate: LocalDate?,
    val nextPaymentDate: LocalDate?,
    val paymentsCount: Int,
    val isActiveInCurrentMonth: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val payments: List<FixedCostPaymentResponse>
)

/**
 * Response DTO dla płatności kosztu stałego
 */
data class FixedCostPaymentResponse(
    val id: String,
    val paymentDate: LocalDate,
    val amount: BigDecimal,
    val plannedAmount: BigDecimal,
    val variance: BigDecimal,
    val status: PaymentStatus,
    val statusDisplay: String,
    val paymentMethod: String?,
    val paymentMethodDisplay: String?,
    val documentId: String?,
    val notes: String?,
    val createdAt: LocalDateTime
)

/**
 * Response DTO dla konfiguracji break-even
 */
data class BreakevenConfigurationResponse(
    val id: String,
    val name: String,
    val description: String?,
    val averageServicePrice: BigDecimal,
    val averageMarginPercentage: BigDecimal,
    val contributionMargin: BigDecimal,
    val variableCost: BigDecimal,
    val workingDaysPerMonth: Int,
    val targetServicesPerDay: Int?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Response DTO dla analizy break-even
 */
data class BreakevenAnalysisResponse(
    val period: LocalDate,
    val configuration: BreakevenConfigurationResponse,
    val totalFixedCosts: BigDecimal,
    val contributionMarginPerService: BigDecimal,
    val breakevenPointServices: Int,
    val breakevenPointRevenue: BigDecimal,
    val requiredServicesPerDay: Int,
    val workingDaysNeeded: Int,
    val isAchievableInMonth: Boolean,
    val costBreakdown: List<CategoryBreakdownItem>,
    val safetyMargin: SafetyMarginResponse,
    val recommendations: List<String>
)

/**
 * Response DTO dla podsumowania kategorii
 */
data class CategorySummaryResponse(
    val categories: List<CategoryBreakdownItem>,
    val totalFixedCosts: BigDecimal,
    val period: LocalDate,
    val activeCostsCount: Int,
    val inactiveCostsCount: Int
)

/**
 * Breakdown kosztu w kategorii
 */
data class CategoryBreakdownItem(
    val category: FixedCostCategory,
    val categoryDisplay: String,
    val totalAmount: BigDecimal,
    val percentage: BigDecimal,
    val activeCosts: Int,
    val trend: TrendDirection,
    val topCosts: List<TopCostItemResponse>
)

/**
 * Top koszt w kategorii
 */
data class TopCostItemResponse(
    val id: String,
    val name: String,
    val amount: BigDecimal,
    val percentage: BigDecimal,
    val status: FixedCostStatus
)

/**
 * Response DTO dla marży bezpieczeństwa
 */
data class SafetyMarginResponse(
    val currentMonthlyRevenue: BigDecimal,
    val safetyMarginAmount: BigDecimal,
    val safetyMarginPercentage: BigDecimal,
    val riskLevel: RiskLevel,
    val riskLevelDisplay: String,
    val riskLevelColor: String
)

/**
 * Response DTO dla nadchodzących płatności
 */
data class UpcomingPaymentsResponse(
    val period: String,
    val totalAmount: BigDecimal,
    val paymentsCount: Int,
    val overdueCount: Int,
    val overdueAmount: BigDecimal,
    val payments: List<UpcomingPaymentItem>
)

/**
 * Pojedyncza nadchodząca płatność
 */
data class UpcomingPaymentItem(
    val fixedCostId: String,
    val fixedCostName: String,
    val category: FixedCostCategory,
    val categoryDisplay: String,
    val dueDate: LocalDate,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val statusDisplay: String,
    val isOverdue: Boolean,
    val daysOverdue: Int?,
    val supplierName: String?
)

/**
 * Response DTO dla projekcji finansowych
 */
data class FinancialProjectionsResponse(
    val projections: List<FinancialProjectionItem>,
    val summary: ProjectionSummary
)

/**
 * Pojedyncza projekcja miesięczna
 */
data class FinancialProjectionItem(
    val month: LocalDate,
    val fixedCosts: BigDecimal,
    val projectedRevenue: BigDecimal,
    val projectedProfit: BigDecimal,
    val breakevenPoint: Int,
    val actualServices: Int?,
    val actualRevenue: BigDecimal?,
    val variance: BigDecimal?,
    val isProfitable: Boolean,
    val coverageRatio: BigDecimal
)

/**
 * Podsumowanie projekcji
 */
data class ProjectionSummary(
    val totalPeriods: Int,
    val profitableMonths: Int,
    val averageFixedCosts: BigDecimal,
    val averageBreakevenPoint: Int,
    val bestMonth: FinancialProjectionItem?,
    val worstMonth: FinancialProjectionItem?
)

// ============ FILTER DTOs ============

/**
 * Filtry do wyszukiwania kosztów stałych
 */
data class FixedCostFilterDTO(
    val name: String? = null,
    val category: FixedCostCategory? = null,
    val status: FixedCostStatus? = null,
    val frequency: CostFrequency? = null,
    val supplierName: String? = null,
    val contractNumber: String? = null,
    val startDateFrom: LocalDate? = null,
    val startDateTo: LocalDate? = null,
    val endDateFrom: LocalDate? = null,
    val endDateTo: LocalDate? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val activeInPeriod: LocalDate? = null
)

// ============ HELPER DTOs ============

/**
 * DTO dla informacji o dostawcy
 */
data class SupplierInfoDTO(
    @field:NotBlank(message = "Supplier name is required")
    @field:Size(max = 255, message = "Supplier name must not exceed 255 characters")
    val name: String,

    @field:Size(max = 50, message = "Tax ID must not exceed 50 characters")
    val taxId: String? = null
)