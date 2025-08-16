package com.carslab.crm.production.modules.vehicles.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleTableResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleWithStatisticsResponse
import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleSearchCriteria
import com.carslab.crm.production.modules.vehicles.domain.service.VehicleDomainService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VehicleQueryService(
    private val vehicleDomainService: VehicleDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VehicleQueryService::class.java)
    
    fun getVehicle(vehicleId: String): VehicleWithStatisticsResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching vehicle: {} for company: {}", vehicleId, companyId)

        val enhancedVehicle = vehicleDomainService.getEnhancedVehicle(VehicleId.of(vehicleId.toLong()), companyId)
        return VehicleWithStatisticsResponse.from(enhancedVehicle)
    }
    
    fun exists(vehicleId: VehicleId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Checking existence of vehicle: {} for company: {}", vehicleId, companyId)

        return vehicleDomainService.exists(vehicleId, companyId)
    }

    fun searchVehicles(
        make: String?,
        model: String?,
        licensePlate: String?,
        vin: String?,
        year: Int?,
        ownerName: String?,
        minVisits: Int?,
        maxVisits: Int?,
        pageable: Pageable
    ): Page<VehicleTableResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Searching vehicles for company: {} with criteria", companyId)

        val searchCriteria = VehicleSearchCriteria(
            make = make,
            model = model,
            licensePlate = licensePlate,
            vin = vin,
            year = year,
            ownerName = ownerName,
            minVisits = minVisits,
            maxVisits = maxVisits
        )

        return vehicleDomainService.getVehicles(companyId, searchCriteria, pageable)
            .let { vehicleDomainService.enhanceVehicles(it) }
            .map { VehicleTableResponse.from(it) }
    }
}