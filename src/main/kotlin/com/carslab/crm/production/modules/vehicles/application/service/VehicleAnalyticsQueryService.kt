// src/main/kotlin/com/carslab/crm/production/modules/vehicles/application/service/VehicleAnalyticsQueryService.kt
package com.carslab.crm.production.modules.vehicles.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleAnalyticsResponse
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleAnalyticsRepository
import com.carslab.crm.production.modules.vehicles.domain.service.VehicleAccessValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VehicleAnalyticsQueryService(
    private val vehicleAnalyticsRepository: VehicleAnalyticsRepository,
    private val vehicleAccessValidator: VehicleAccessValidator,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VehicleAnalyticsQueryService::class.java)

    fun getVehicleAnalytics(vehicleId: String): VehicleAnalyticsResponse {
        val companyId = securityContext.getCurrentCompanyId()
        val vehicleIdObj = VehicleId.of(vehicleId.toLong())

        logger.debug("Getting analytics for vehicle: {} in company: {}", vehicleId, companyId)

        // Validate access to vehicle
        vehicleAccessValidator.getVehicleForCompany(vehicleIdObj, companyId)

        val profitabilityAnalysis = vehicleAnalyticsRepository.getProfitabilityAnalysis(vehicleIdObj, companyId)
        val visitPattern = vehicleAnalyticsRepository.getVisitPattern(vehicleIdObj, companyId)
        val servicePreferences = vehicleAnalyticsRepository.getServicePreferences(vehicleIdObj, companyId)

        logger.debug("Successfully retrieved analytics for vehicle: {}", vehicleId)

        return VehicleAnalyticsResponse.from(
            profitabilityAnalysis = profitabilityAnalysis,
            visitPattern = visitPattern,
            servicePreferences = servicePreferences
        )
    }

    fun getBatchVehicleAnalytics(vehicleIds: List<String>): Map<String, VehicleAnalyticsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val vehicleIdObjs = vehicleIds.map { VehicleId.of(it.toLong()) }

        logger.debug("Getting batch analytics for {} vehicles in company: {}", vehicleIds.size, companyId)

        val profitabilityMap = vehicleAnalyticsRepository.getBatchProfitabilityAnalysis(vehicleIdObjs, companyId)
        val visitPatternMap = vehicleAnalyticsRepository.getBatchVisitPatterns(vehicleIdObjs, companyId)
        val servicePreferencesMap = vehicleAnalyticsRepository.getBatchServicePreferences(vehicleIdObjs, companyId)

        return vehicleIds.associateWith { vehicleId ->
            val vehicleIdObj = VehicleId.of(vehicleId.toLong())
            VehicleAnalyticsResponse.from(
                profitabilityAnalysis = profitabilityMap[vehicleIdObj],
                visitPattern = visitPatternMap[vehicleIdObj],
                servicePreferences = servicePreferencesMap[vehicleIdObj]
            )
        }
    }
}