// src/main/kotlin/com/carslab/crm/production/modules/vehicles/domain/model/VehicleAnalytics.kt
package com.carslab.crm.production.modules.vehicles.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class VehicleProfitabilityAnalysis(
    val vehicleId: VehicleId,
    val companyId: Long,
    val averageVisitValue: BigDecimal,
    val monthlyRevenue: BigDecimal,
    val revenueTrend: RevenueTrend,
    val trendPercentage: BigDecimal,
    val profitabilityScore: Int,
    val calculatedAt: LocalDateTime
) {
    companion object {
        fun calculateProfitabilityScore(
            averageVisitValue: BigDecimal,
            visitFrequencyPerMonth: Double,
            monthsSinceFirstVisit: Int
        ): Int {
            val valueScore = (averageVisitValue.toDouble() / 500.0).coerceAtMost(1.0) * 3.0
            val frequencyScore = (visitFrequencyPerMonth / 2.0).coerceAtMost(1.0) * 4.0
            val loyaltyScore = (monthsSinceFirstVisit / 24.0).coerceAtMost(1.0) * 3.0

            return ((valueScore + frequencyScore + loyaltyScore) * 10 / 10).toInt().coerceIn(1, 10)
        }
    }
}

data class VehicleVisitPattern(
    val vehicleId: VehicleId,
    val companyId: Long,
    val daysSinceLastVisit: Int?,
    val averageDaysBetweenVisits: Int?,
    val visitRegularityStatus: VisitRegularityStatus,
    val nextRecommendedVisitDate: LocalDate?,
    val totalVisits: Int,
    val calculatedAt: LocalDateTime
)

data class VehicleServicePreferences(
    val vehicleId: VehicleId,
    val companyId: Long,
    val topServices: List<ServiceUsageSummary>,
    val calculatedAt: LocalDateTime
)

data class ServiceUsageSummary(
    val serviceId: String,
    val serviceName: String,
    val usageCount: Int,
    val totalRevenue: BigDecimal,
    val averagePrice: BigDecimal,
    val lastUsedDate: LocalDateTime?
)

enum class RevenueTrend(val displayName: String, val changeIndicator: String) {
    GROWING("Rosnący", "POSITIVE"),
    STABLE("Stabilny", "NEUTRAL"),
    DECLINING("Malejący", "NEGATIVE"),
    INSUFFICIENT_DATA("Brak danych", "UNKNOWN")
}

enum class VisitRegularityStatus(val displayName: String, val riskLevel: String) {
    REGULAR("Regularny klient", "LOW"),
    IRREGULAR("Nieregularny", "MEDIUM"),
    AT_RISK("Zagrożony odejściem", "HIGH"),
    NEW_CLIENT("Nowy klient", "LOW"),
    LOST("Utracony klient", "CRITICAL")
}