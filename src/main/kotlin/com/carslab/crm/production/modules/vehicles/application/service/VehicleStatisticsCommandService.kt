package com.carslab.crm.production.modules.vehicles.application.service

import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.service.VehicleDomainService
import org.springframework.stereotype.Service

@Service
class VehicleStatisticsCommandService(
    private val vehicleDomainService: VehicleDomainService
) {

    fun recordVisit(vehicleId: VehicleId) {
        vehicleDomainService.registerVisit(vehicleId)
    }
    
}