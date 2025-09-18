package com.carslab.crm.production.modules.clients.domain.repository

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.*
import java.time.LocalDate
import java.math.BigDecimal

interface ClientAnalyticsRepository {

    /**
     * Get basic analytics for a single client
     */
    fun getClientAnalytics(clientId: ClientId, companyId: Long): ClientAnalytics?

    /**
     * Get revenue trend comparison (last 3 vs previous 3 months)
     */
    fun getRevenueTrend(clientId: ClientId, companyId: Long): ClientRevenueTrend?

    /**
     * Get seasonality analysis showing monthly visit patterns
     */
    fun getSeasonalityAnalysis(clientId: ClientId, companyId: Long): ClientSeasonalityAnalysis

    /**
     * Get top services used by client
     */
    fun getTopServices(clientId: ClientId, companyId: Long, limit: Int = 3): List<ClientServiceUsage>

    /**
     * Get referral source breakdown for client
     */
    fun getReferralSources(clientId: ClientId, companyId: Long): List<ClientReferralSource>

    /**
     * Get client growth over time (monthly revenue progression)
     */
    fun getClientGrowthChart(clientId: ClientId, companyId: Long, months: Int = 12): List<ClientMonthlyRevenue>

    /**
     * Get company averages for comparison widget
     */
    fun getCompanyAverages(companyId: Long): CompanyAverages

    /**
     * Batch analytics for multiple clients (for performance)
     */
    fun getBatchClientAnalytics(clientIds: List<ClientId>, companyId: Long): Map<ClientId, ClientAnalytics>

    /**
     * Get client comparison metrics vs company average
     */
    fun getClientComparison(clientId: ClientId, companyId: Long): ClientComparisonMetrics?
}