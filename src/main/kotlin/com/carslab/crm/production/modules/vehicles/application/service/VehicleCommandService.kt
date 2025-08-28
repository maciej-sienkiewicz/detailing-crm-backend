package com.carslab.crm.production.modules.vehicles.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.associations.domain.service.AssociationDomainService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.application.dto.CreateVehicleRequest
import com.carslab.crm.production.modules.vehicles.application.dto.UpdateVehicleRequest
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.vehicles.domain.command.CreateVehicleCommand
import com.carslab.crm.production.modules.vehicles.domain.command.UpdateVehicleCommand
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.service.VehicleDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VehicleCommandService(
    private val vehicleDomainService: VehicleDomainService,
    private val associationDomainService: AssociationDomainService,
    private val securityContext: SecurityContext,
    private val vehicleInputValidator: VehicleInputValidator
) {
    private val logger = LoggerFactory.getLogger(VehicleCommandService::class.java)

    fun createVehicle(request: CreateVehicleRequest): VehicleResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating vehicle '{}' for company: {}", request.make, companyId)

        vehicleInputValidator.validateCreateRequest(request)

        val command = CreateVehicleCommand(
            companyId = companyId,
            make = request.make,
            model = request.model,
            year = request.year,
            licensePlate = request.licensePlate,
            color = request.color,
            vin = request.vin,
            mileage = request.mileage,
            ownerIds = request.ownerIds
        )

        val vehicle = vehicleDomainService.createVehicle(command)

        request.ownerIds.forEachIndexed { index, ownerId ->
            associationDomainService.createAssociation(
                clientId = ClientId.of(ownerId),
                vehicleId = vehicle.id,
                companyId = companyId,
                isPrimary = index == 0 && request.ownerIds.size == 1
            )
        }

        logger.info("Vehicle created successfully: {}", vehicle.id.value)
        return VehicleResponse.from(vehicle)
    }

    fun updateVehicle(vehicleId: String, request: UpdateVehicleRequest): VehicleResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating vehicle: {} for company: {}", vehicleId, companyId)

        vehicleInputValidator.validateUpdateRequest(request)

        val command = UpdateVehicleCommand(
            make = request.make,
            model = request.model,
            year = request.year,
            licensePlate = request.licensePlate,
            color = request.color,
            vin = request.vin,
            mileage = request.mileage,
            ownerIds = request.ownerIds.map { ClientId(it) }
        )

        val vehicle = vehicleDomainService.updateVehicle(VehicleId.of(vehicleId.toLong()), command, companyId)
        logger.info("Vehicle updated successfully: {}", vehicleId)

        return VehicleResponse.from(vehicle)
    }

    fun deleteVehicle(vehicleId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting vehicle: {} for company: {}", vehicleId, companyId)

        val deleted = vehicleDomainService.deleteVehicle(VehicleId.of(vehicleId.toLong()), companyId)
        if (deleted) {
            logger.info("Vehicle deleted successfully: {}", vehicleId)
        } else {
            logger.warn("Vehicle not found for deletion: {}", vehicleId)
        }
    }
}