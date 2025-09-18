package com.carslab.crm.production.modules.clients.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.clients.application.dto.ClientAnalyticsResponse
import com.carslab.crm.production.modules.clients.application.dto.ClientAnalyticsSummaryResponse
import com.carslab.crm.production.modules.clients.application.dto.CompanyAveragesResponse
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientAnalyticsRepository
import com.carslab.crm.production.modules.clients.domain.service.ClientAccessValidator
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ClientAnalyticsService(
    private val clientAnalyticsRepository: ClientAnalyticsRepository,
    private val clientAccessValidator: ClientAccessValidator,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ClientAnalyticsService::class.java)
    
    fun getClientAnalytics(clientId: ClientId): ClientAnalyticsResponse? {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting analytics for client: {} in company: {}", clientId.value, companyId)

        // Verify client access
        clientAccessValidator.getClientForCompany(clientId, companyId)

        val analytics = clientAnalyticsRepository.getClientAnalytics(clientId, companyId)
            ?: return null

        val revenueTrend = clientAnalyticsRepository.getRevenueTrend(clientId, companyId)
        val seasonality = clientAnalyticsRepository.getSeasonalityAnalysis(clientId, companyId)
        val topServices = clientAnalyticsRepository.getTopServices(clientId, companyId, 3)
        val referralSources = clientAnalyticsRepository.getReferralSources(clientId, companyId)
        val growthChart = clientAnalyticsRepository.getClientGrowthChart(clientId, companyId, 12)
        val comparison = clientAnalyticsRepository.getClientComparison(clientId, companyId)

        logger.debug("Successfully retrieved analytics for client: {}", clientId.value)

        return ClientAnalyticsResponse.from(
            analytics = analytics,
            revenueTrend = revenueTrend,
            seasonality = seasonality,
            topServices = topServices,
            referralSources = referralSources,
            growthChart = growthChart,
            comparison = comparison
        )
    }

    @Cacheable(
        value = ["company-averages"],
        key = "#root.target.securityContext.currentCompanyId",
        unless = "#result == null"
    )
    fun getCompanyAverages(): CompanyAveragesResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting company averages for company: {}", companyId)

        val averages = clientAnalyticsRepository.getCompanyAverages(companyId)

        logger.debug("Successfully retrieved company averages for company: {}", companyId)
        return CompanyAveragesResponse.from(averages)
    }

    fun getBatchClientAnalytics(clientIds: List<ClientId>): Map<ClientId, ClientAnalyticsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting batch analytics for {} clients in company: {}", clientIds.size, companyId)

        if (clientIds.isEmpty()) return emptyMap()

        // Verify all clients belong to the company (batch validation would be more efficient)
        clientIds.forEach { clientId ->
            clientAccessValidator.getClientForCompany(clientId, companyId)
        }

        val batchAnalytics = clientAnalyticsRepository.getBatchClientAnalytics(clientIds, companyId)

        // For batch operations, we might want to simplify and only include basic analytics
        // to avoid performance issues with complex queries for each client
        return batchAnalytics.mapValues { (clientId, analytics) ->
            ClientAnalyticsResponse.fromBasic(analytics)
        }
    }

    /**
     * Lightweight version for dashboard/list views
     */
    fun getClientAnalyticsSummary(clientId: ClientId): ClientAnalyticsSummaryResponse? {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting analytics summary for client: {} in company: {}", clientId.value, companyId)

        clientAccessValidator.getClientForCompany(clientId, companyId)

        val analytics = clientAnalyticsRepository.getClientAnalytics(clientId, companyId)
            ?: return null

        val comparison = clientAnalyticsRepository.getClientComparison(clientId, companyId)

        return ClientAnalyticsSummaryResponse.from(analytics, comparison)
    }
}