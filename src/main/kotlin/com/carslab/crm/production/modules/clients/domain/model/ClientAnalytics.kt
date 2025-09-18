package com.carslab.crm.production.modules.clients.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Month

/**
 * Core analytics data for a client
 */
data class ClientAnalytics(
    val clientId: ClientId,
    val companyId: Long,
    val averageVisitValue: BigDecimal,
    val totalRevenue: BigDecimal,
    val totalVisits: Int,
    val monthsSinceFirstVisit: Int,
    val daysSinceLastVisit: Int?,
    val calculatedAt: LocalDateTime
)

/**
 * Revenue trend comparing recent vs previous periods
 */
data class ClientRevenueTrend(
    val clientId: ClientId,
    val companyId: Long,
    val recentRevenue: BigDecimal, // Last 3 months
    val previousRevenue: BigDecimal, // Previous 3 months
    val trendPercentage: BigDecimal, // -100 to +∞
    val trendDirection: TrendDirection,
    val calculatedAt: LocalDateTime
) {
    companion object {
        fun calculateTrendDirection(percentage: BigDecimal): TrendDirection {
            return when {
                percentage > BigDecimal("15") -> TrendDirection.STRONG_GROWTH
                percentage > BigDecimal("5") -> TrendDirection.GROWTH
                percentage >= BigDecimal("-5") -> TrendDirection.STABLE
                percentage >= BigDecimal("-15") -> TrendDirection.DECLINE
                else -> TrendDirection.STRONG_DECLINE
            }
        }
    }
}

enum class TrendDirection {
    STRONG_GROWTH, GROWTH, STABLE, DECLINE, STRONG_DECLINE
}

/**
 * Monthly seasonality patterns
 */
data class ClientSeasonalityAnalysis(
    val clientId: ClientId,
    val companyId: Long,
    val monthlyData: List<MonthlyVisitData>,
    val peakMonth: Month?,
    val leastActiveMonth: Month?,
    val calculatedAt: LocalDateTime
)

data class MonthlyVisitData(
    val month: Month,
    val visitCount: Int,
    val revenue: BigDecimal,
    val averageVisitValue: BigDecimal
)

/**
 * Service usage statistics
 */
data class ClientServiceUsage(
    val serviceId: String,
    val serviceName: String,
    val usageCount: Int,
    val totalRevenue: BigDecimal,
    val averagePrice: BigDecimal,
    val lastUsedDate: LocalDateTime?
)

/**
 * Referral source breakdown
 */
data class ClientReferralSource(
    val source: String, // REFERRAL, ORGANIC, ADVERTISING, etc.
    val visitCount: Int,
    val firstVisitDate: LocalDateTime,
    val totalRevenue: BigDecimal
)

/**
 * Monthly revenue progression for growth chart
 */
data class ClientMonthlyRevenue(
    val year: Int,
    val month: Month,
    val revenue: BigDecimal,
    val visitCount: Int,
    val cumulativeRevenue: BigDecimal
)

/**
 * Company-wide averages for comparison
 */
data class CompanyAverages(
    val companyId: Long,
    val averageVisitValue: BigDecimal,
    val averageMonthlyRevenue: BigDecimal,
    val averageVisitsPerMonth: Double,
    val averageClientLifespanMonths: Int,
    val calculatedAt: LocalDateTime
)

/**
 * Client vs company comparison metrics
 */
data class ClientComparisonMetrics(
    val clientId: ClientId,
    val companyId: Long,
    val visitValueVsAverage: ComparisonMetric,
    val monthlyRevenueVsAverage: ComparisonMetric,
    val visitsFrequencyVsAverage: ComparisonMetric,
    val lifespanVsAverage: ComparisonMetric,
    val overallScore: ClientScore, // VIP, HIGH_VALUE, AVERAGE, LOW_VALUE, AT_RISK
    val calculatedAt: LocalDateTime
)

data class ComparisonMetric(
    val clientValue: BigDecimal,
    val companyAverage: BigDecimal,
    val percentageDifference: BigDecimal, // Can be negative
    val performanceLevel: PerformanceLevel
)

enum class PerformanceLevel {
    EXCELLENT,    // >50% above average
    GOOD,        // 20-50% above average  
    AVERAGE,     // -20% to +20% of average
    BELOW,       // -50% to -20% of average
    POOR         // <-50% of average
}

enum class ClientScore {
    VIP,          // Top 5% clients
    HIGH_VALUE,   // Top 20% clients
    AVERAGE,      // Middle 60% clients  
    LOW_VALUE,    // Bottom 15% clients
    AT_RISK       // Clients with declining patterns
}