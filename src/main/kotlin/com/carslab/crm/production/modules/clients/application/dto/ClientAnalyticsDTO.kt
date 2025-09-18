package com.carslab.crm.production.modules.clients.application.dto

import com.carslab.crm.production.modules.clients.domain.model.*
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Month

data class ClientAnalyticsResponse(
    @JsonProperty("basic_metrics")
    val basicMetrics: BasicMetricsResponse,

    @JsonProperty("revenue_trend")
    val revenueTrend: RevenueTrendResponse?,

    @JsonProperty("seasonality")
    val seasonality: SeasonalityResponse,

    @JsonProperty("top_services")
    val topServices: List<ServiceUsageResponse>,

    @JsonProperty("referral_sources")
    val referralSources: List<ReferralSourceResponse>,

    @JsonProperty("growth_chart")
    val growthChart: List<MonthlyRevenueResponse>,

    @JsonProperty("comparison")
    val comparison: ClientComparisonResponse?,

    @JsonProperty("calculated_at")
    val calculatedAt: LocalDateTime
) {
    companion object {
        fun from(
            analytics: ClientAnalytics,
            revenueTrend: ClientRevenueTrend?,
            seasonality: ClientSeasonalityAnalysis,
            topServices: List<ClientServiceUsage>,
            referralSources: List<ClientReferralSource>,
            growthChart: List<ClientMonthlyRevenue>,
            comparison: ClientComparisonMetrics?
        ): ClientAnalyticsResponse {
            return ClientAnalyticsResponse(
                basicMetrics = BasicMetricsResponse.from(analytics),
                revenueTrend = revenueTrend?.let { RevenueTrendResponse.from(it) },
                seasonality = SeasonalityResponse.from(seasonality),
                topServices = topServices.map { ServiceUsageResponse.from(it) },
                referralSources = referralSources.map { ReferralSourceResponse.from(it) },
                growthChart = growthChart.map { MonthlyRevenueResponse.from(it) },
                comparison = comparison?.let { ClientComparisonResponse.from(it) },
                calculatedAt = analytics.calculatedAt
            )
        }

        fun fromBasic(analytics: ClientAnalytics): ClientAnalyticsResponse {
            return ClientAnalyticsResponse(
                basicMetrics = BasicMetricsResponse.from(analytics),
                revenueTrend = null,
                seasonality = SeasonalityResponse(emptyList(), null, null, analytics.calculatedAt),
                topServices = emptyList(),
                referralSources = emptyList(),
                growthChart = emptyList(),
                comparison = null,
                calculatedAt = analytics.calculatedAt
            )
        }
    }
}

data class BasicMetricsResponse(
    @JsonProperty("average_visit_value")
    val averageVisitValue: BigDecimal,

    @JsonProperty("total_revenue")
    val totalRevenue: BigDecimal,

    @JsonProperty("total_visits")
    val totalVisits: Int,

    @JsonProperty("months_since_first_visit")
    val monthsSinceFirstVisit: Int,

    @JsonProperty("days_since_last_visit")
    val daysSinceLastVisit: Int?
) {
    companion object {
        fun from(analytics: ClientAnalytics): BasicMetricsResponse {
            return BasicMetricsResponse(
                averageVisitValue = analytics.averageVisitValue,
                totalRevenue = analytics.totalRevenue,
                totalVisits = analytics.totalVisits,
                monthsSinceFirstVisit = analytics.monthsSinceFirstVisit,
                daysSinceLastVisit = analytics.daysSinceLastVisit
            )
        }
    }
}

data class RevenueTrendResponse(
    @JsonProperty("recent_revenue")
    val recentRevenue: BigDecimal,

    @JsonProperty("previous_revenue")
    val previousRevenue: BigDecimal,

    @JsonProperty("trend_percentage")
    val trendPercentage: BigDecimal,

    @JsonProperty("trend_direction")
    val trendDirection: String,

    @JsonProperty("trend_description")
    val trendDescription: String
) {
    companion object {
        fun from(trend: ClientRevenueTrend): RevenueTrendResponse {
            val description = when (trend.trendDirection) {
                TrendDirection.STRONG_GROWTH -> "Silny wzrost przychodów"
                TrendDirection.GROWTH -> "Wzrost przychodów"
                TrendDirection.STABLE -> "Stabilne przychody"
                TrendDirection.DECLINE -> "Spadek przychodów"
                TrendDirection.STRONG_DECLINE -> "Silny spadek przychodów"
            }

            return RevenueTrendResponse(
                recentRevenue = trend.recentRevenue,
                previousRevenue = trend.previousRevenue,
                trendPercentage = trend.trendPercentage,
                trendDirection = trend.trendDirection.name,
                trendDescription = description
            )
        }
    }
}

data class SeasonalityResponse(
    @JsonProperty("monthly_data")
    val monthlyData: List<MonthlyDataResponse>,

    @JsonProperty("peak_month")
    val peakMonth: String?,

    @JsonProperty("least_active_month")
    val leastActiveMonth: String?,

    @JsonProperty("calculated_at")
    val calculatedAt: LocalDateTime
) {
    companion object {
        fun from(seasonality: ClientSeasonalityAnalysis): SeasonalityResponse {
            return SeasonalityResponse(
                monthlyData = seasonality.monthlyData.map { MonthlyDataResponse.from(it) },
                peakMonth = seasonality.peakMonth?.name,
                leastActiveMonth = seasonality.leastActiveMonth?.name,
                calculatedAt = seasonality.calculatedAt
            )
        }
    }
}

data class MonthlyDataResponse(
    val month: String,
    @JsonProperty("month_number")
    val monthNumber: Int,
    @JsonProperty("visit_count")
    val visitCount: Int,
    val revenue: BigDecimal,
    @JsonProperty("average_visit_value")
    val averageVisitValue: BigDecimal
) {
    companion object {
        fun from(data: MonthlyVisitData): MonthlyDataResponse {
            return MonthlyDataResponse(
                month = data.month.name,
                monthNumber = data.month.value,
                visitCount = data.visitCount,
                revenue = data.revenue,
                averageVisitValue = data.averageVisitValue
            )
        }
    }
}

data class ServiceUsageResponse(
    @JsonProperty("service_id")
    val serviceId: String,
    @JsonProperty("service_name")
    val serviceName: String,
    @JsonProperty("usage_count")
    val usageCount: Int,
    @JsonProperty("total_revenue")
    val totalRevenue: BigDecimal,
    @JsonProperty("average_price")
    val averagePrice: BigDecimal,
    @JsonProperty("last_used_date")
    val lastUsedDate: LocalDateTime?
) {
    companion object {
        fun from(usage: ClientServiceUsage): ServiceUsageResponse {
            return ServiceUsageResponse(
                serviceId = usage.serviceId,
                serviceName = usage.serviceName,
                usageCount = usage.usageCount,
                totalRevenue = usage.totalRevenue,
                averagePrice = usage.averagePrice,
                lastUsedDate = usage.lastUsedDate
            )
        }
    }
}

data class ReferralSourceResponse(
    val source: String,
    @JsonProperty("source_display_name")
    val sourceDisplayName: String,
    @JsonProperty("visit_count")
    val visitCount: Int,
    @JsonProperty("first_visit_date")
    val firstVisitDate: LocalDateTime,
    @JsonProperty("total_revenue")
    val totalRevenue: BigDecimal
) {
    companion object {
        fun from(referral: ClientReferralSource): ReferralSourceResponse {
            val displayName = when (referral.source) {
                "REFERRAL" -> "Polecenie"
                "ORGANIC" -> "Organiczne"
                "ADVERTISING" -> "Reklama"
                "SOCIAL_MEDIA" -> "Media społecznościowe"
                "GOOGLE" -> "Google"
                "UNKNOWN" -> "Nieznane"
                else -> referral.source
            }

            return ReferralSourceResponse(
                source = referral.source,
                sourceDisplayName = displayName,
                visitCount = referral.visitCount,
                firstVisitDate = referral.firstVisitDate,
                totalRevenue = referral.totalRevenue
            )
        }
    }
}

data class MonthlyRevenueResponse(
    val year: Int,
    val month: String,
    @JsonProperty("month_number")
    val monthNumber: Int,
    val revenue: BigDecimal,
    @JsonProperty("visit_count")
    val visitCount: Int,
    @JsonProperty("cumulative_revenue")
    val cumulativeRevenue: BigDecimal
) {
    companion object {
        fun from(monthly: ClientMonthlyRevenue): MonthlyRevenueResponse {
            return MonthlyRevenueResponse(
                year = monthly.year,
                month = monthly.month.name,
                monthNumber = monthly.month.value,
                revenue = monthly.revenue,
                visitCount = monthly.visitCount,
                cumulativeRevenue = monthly.cumulativeRevenue
            )
        }
    }
}

data class ClientComparisonResponse(
    @JsonProperty("visit_value_comparison")
    val visitValueComparison: ComparisonMetricResponse,

    @JsonProperty("monthly_revenue_comparison")
    val monthlyRevenueComparison: ComparisonMetricResponse,

    @JsonProperty("visits_frequency_comparison")
    val visitsFrequencyComparison: ComparisonMetricResponse,

    @JsonProperty("lifespan_comparison")
    val lifespanComparison: ComparisonMetricResponse,

    @JsonProperty("overall_score")
    val overallScore: String,

    @JsonProperty("score_description")
    val scoreDescription: String
) {
    companion object {
        fun from(comparison: ClientComparisonMetrics): ClientComparisonResponse {
            val scoreDescription = when (comparison.overallScore) {
                ClientScore.VIP -> "Klient VIP - należy do top 5% najlepszych klientów"
                ClientScore.HIGH_VALUE -> "Klient wysokiej wartości - należy do top 20% klientów"
                ClientScore.AVERAGE -> "Klient przeciętny - wyniki zgodne ze średnią firmową"
                ClientScore.LOW_VALUE -> "Klient niskiej wartości - poniżej średniej firmowej"
                ClientScore.AT_RISK -> "Klient zagrożony - spadające wskaźniki, wymaga uwagi"
            }

            return ClientComparisonResponse(
                visitValueComparison = ComparisonMetricResponse.from(comparison.visitValueVsAverage),
                monthlyRevenueComparison = ComparisonMetricResponse.from(comparison.monthlyRevenueVsAverage),
                visitsFrequencyComparison = ComparisonMetricResponse.from(comparison.visitsFrequencyVsAverage),
                lifespanComparison = ComparisonMetricResponse.from(comparison.lifespanVsAverage),
                overallScore = comparison.overallScore.name,
                scoreDescription = scoreDescription
            )
        }
    }
}

data class ComparisonMetricResponse(
    @JsonProperty("client_value")
    val clientValue: BigDecimal,

    @JsonProperty("company_average")
    val companyAverage: BigDecimal,

    @JsonProperty("percentage_difference")
    val percentageDifference: BigDecimal,

    @JsonProperty("performance_level")
    val performanceLevel: String,

    @JsonProperty("performance_description")
    val performanceDescription: String
) {
    companion object {
        fun from(metric: ComparisonMetric): ComparisonMetricResponse {
            val description = when (metric.performanceLevel) {
                PerformanceLevel.EXCELLENT -> "Wybitny - ponad 50% powyżej średniej"
                PerformanceLevel.GOOD -> "Dobry - 20-50% powyżej średniej"
                PerformanceLevel.AVERAGE -> "Przeciętny - zgodny ze średnią"
                PerformanceLevel.BELOW -> "Poniżej średniej - 20-50% poniżej"
                PerformanceLevel.POOR -> "Słaby - ponad 50% poniżej średniej"
            }

            return ComparisonMetricResponse(
                clientValue = metric.clientValue,
                companyAverage = metric.companyAverage,
                percentageDifference = metric.percentageDifference,
                performanceLevel = metric.performanceLevel.name,
                performanceDescription = description
            )
        }
    }
}

data class CompanyAveragesResponse(
    @JsonProperty("average_visit_value")
    val averageVisitValue: BigDecimal,

    @JsonProperty("average_monthly_revenue")
    val averageMonthlyRevenue: BigDecimal,

    @JsonProperty("average_visits_per_month")
    val averageVisitsPerMonth: Double,

    @JsonProperty("average_client_lifespan_months")
    val averageClientLifespanMonths: Int,

    @JsonProperty("calculated_at")
    val calculatedAt: LocalDateTime
) {
    companion object {
        fun from(averages: CompanyAverages): CompanyAveragesResponse {
            return CompanyAveragesResponse(
                averageVisitValue = averages.averageVisitValue,
                averageMonthlyRevenue = averages.averageMonthlyRevenue,
                averageVisitsPerMonth = averages.averageVisitsPerMonth,
                averageClientLifespanMonths = averages.averageClientLifespanMonths,
                calculatedAt = averages.calculatedAt
            )
        }
    }
}

/**
 * Lightweight summary for list views and dashboards
 */
data class ClientAnalyticsSummaryResponse(
    @JsonProperty("average_visit_value")
    val averageVisitValue: BigDecimal,

    @JsonProperty("total_revenue")
    val totalRevenue: BigDecimal,

    @JsonProperty("total_visits")
    val totalVisits: Int,

    @JsonProperty("client_score")
    val clientScore: String?,

    @JsonProperty("score_badge_color")
    val scoreBadgeColor: String,

    @JsonProperty("days_since_last_visit")
    val daysSinceLastVisit: Int?
) {
    companion object {
        fun from(analytics: ClientAnalytics, comparison: ClientComparisonMetrics?): ClientAnalyticsSummaryResponse {
            val (score, badgeColor) = when (comparison?.overallScore) {
                ClientScore.VIP -> "VIP" to "purple"
                ClientScore.HIGH_VALUE -> "HIGH" to "blue"
                ClientScore.AVERAGE -> "AVG" to "green"
                ClientScore.LOW_VALUE -> "LOW" to "orange"
                ClientScore.AT_RISK -> "RISK" to "red"
                null -> null to "gray"
            }

            return ClientAnalyticsSummaryResponse(
                averageVisitValue = analytics.averageVisitValue,
                totalRevenue = analytics.totalRevenue,
                totalVisits = analytics.totalVisits,
                clientScore = score,
                scoreBadgeColor = badgeColor,
                daysSinceLastVisit = analytics.daysSinceLastVisit
            )
        }
    }
}