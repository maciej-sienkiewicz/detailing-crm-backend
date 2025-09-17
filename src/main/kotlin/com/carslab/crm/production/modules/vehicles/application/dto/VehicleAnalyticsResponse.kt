package com.carslab.crm.production.modules.vehicles.application.dto

import com.carslab.crm.production.modules.vehicles.domain.model.*
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class VehicleAnalyticsResponse(
    val profitabilityAnalysis: ProfitabilityAnalysisDto?,
    val visitPattern: VisitPatternDto?,
    val servicePreferences: ServicePreferencesDto?
) {
    companion object {
        fun from(
            profitabilityAnalysis: VehicleProfitabilityAnalysis?,
            visitPattern: VehicleVisitPattern?,
            servicePreferences: VehicleServicePreferences?
        ): VehicleAnalyticsResponse {
            return VehicleAnalyticsResponse(
                profitabilityAnalysis = profitabilityAnalysis?.let { ProfitabilityAnalysisDto.from(it) },
                visitPattern = visitPattern?.let { VisitPatternDto.from(it) },
                servicePreferences = servicePreferences?.let { ServicePreferencesDto.from(it) }
            )
        }
    }
}

data class ProfitabilityAnalysisDto(
    @JsonProperty("average_visit_value")
    val averageVisitValue: BigDecimal,
    @JsonProperty("monthly_revenue")
    val monthlyRevenue: BigDecimal,
    @JsonProperty("revenue_trend")
    val revenueTrend: String,
    @JsonProperty("trend_percentage")
    val trendPercentage: BigDecimal,
    @JsonProperty("trend_display_name")
    val trendDisplayName: String,
    @JsonProperty("trend_change_indicator")
    val trendChangeIndicator: String,
    @JsonProperty("profitability_score")
    val profitabilityScore: Int
) {
    companion object {
        fun from(analysis: VehicleProfitabilityAnalysis): ProfitabilityAnalysisDto {
            return ProfitabilityAnalysisDto(
                averageVisitValue = analysis.averageVisitValue,
                monthlyRevenue = analysis.monthlyRevenue,
                revenueTrend = analysis.revenueTrend.name,
                trendPercentage = analysis.trendPercentage,
                trendDisplayName = analysis.revenueTrend.displayName,
                trendChangeIndicator = analysis.revenueTrend.changeIndicator,
                profitabilityScore = analysis.profitabilityScore
            )
        }
    }
}

data class VisitPatternDto(
    @JsonProperty("days_since_last_visit")
    val daysSinceLastVisit: Int?,
    @JsonProperty("average_days_between_visits")
    val averageDaysBetweenVisits: Int?,
    @JsonProperty("visit_regularity_status")
    val visitRegularityStatus: String,
    @JsonProperty("regularity_display_name")
    val regularityDisplayName: String,
    @JsonProperty("risk_level")
    val riskLevel: String,
    @JsonProperty("next_recommended_visit_date")
    val nextRecommendedVisitDate: LocalDate?,
    @JsonProperty("total_visits")
    val totalVisits: Int
) {
    companion object {
        fun from(pattern: VehicleVisitPattern): VisitPatternDto {
            return VisitPatternDto(
                daysSinceLastVisit = pattern.daysSinceLastVisit,
                averageDaysBetweenVisits = pattern.averageDaysBetweenVisits,
                visitRegularityStatus = pattern.visitRegularityStatus.name,
                regularityDisplayName = pattern.visitRegularityStatus.displayName,
                riskLevel = pattern.visitRegularityStatus.riskLevel,
                nextRecommendedVisitDate = pattern.nextRecommendedVisitDate,
                totalVisits = pattern.totalVisits
            )
        }
    }
}

data class ServicePreferencesDto(
    @JsonProperty("top_services")
    val topServices: List<ServiceUsageDto>
) {
    companion object {
        fun from(preferences: VehicleServicePreferences): ServicePreferencesDto {
            return ServicePreferencesDto(
                topServices = preferences.topServices.map { ServiceUsageDto.from(it) }
            )
        }
    }
}

data class ServiceUsageDto(
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
        fun from(summary: ServiceUsageSummary): ServiceUsageDto {
            return ServiceUsageDto(
                serviceId = summary.serviceId,
                serviceName = summary.serviceName,
                usageCount = summary.usageCount,
                totalRevenue = summary.totalRevenue,
                averagePrice = summary.averagePrice,
                lastUsedDate = summary.lastUsedDate
            )
        }
    }
}