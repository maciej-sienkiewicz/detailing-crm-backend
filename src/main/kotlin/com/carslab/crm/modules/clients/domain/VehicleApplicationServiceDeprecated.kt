package com.carslab.crm.modules.clients.domain

import com.carslab.crm.modules.clients.api.CreateVehicleCommand
import com.carslab.crm.modules.clients.api.UpdateVehicleCommand
import com.carslab.crm.domain.exception.DomainException
import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.modules.clients.domain.model.VehicleRelationshipType
import com.carslab.crm.modules.clients.domain.port.VehicleSearchCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class VehicleApplicationServiceDeprecated(
    private val vehicleDomainServiceDeprecated: VehicleDomainServiceDeprecated,
    private val associationService: ClientVehicleAssociationService
) {
    private val logger = LoggerFactory.getLogger(VehicleApplicationServiceDeprecated::class.java)

    fun createVehicle(request: CreateVehicleRequest): VehicleDetailResponse {
        logger.info("Creating vehicle: ${request.make} ${request.model} (${request.licensePlate})")

        try {
            validateCreateVehicleRequest(request)

            val command = CreateVehicleCommand(
                make = request.make,
                model = request.model,
                year = request.year ?: 2024, // Default year if null
                licensePlate = request.licensePlate,
                color = request.color,
                vin = request.vin,
                mileage = request.mileage,
                ownerIds = request.ownerIds.map { it.toString() }
            )

            val vehicle = vehicleDomainServiceDeprecated.createVehicle(command)

            // Associate with owners if provided
            request.ownerIds.forEach { ownerId ->
                try {
                    associationService.associateClientWithVehicle(
                        ClientId.of(ownerId),
                        vehicle.id,
                        VehicleRelationshipType.OWNER,
                        isPrimary = request.ownerIds.size == 1
                    )
                } catch (e: DomainException) {
                    logger.warn("Failed to associate client $ownerId with vehicle ${vehicle.id.value}: ${e.message}")
                    // Continue with other associations
                }
            }

            logger.info("Successfully created vehicle with ID: ${vehicle.id.value}")

            // Get vehicle with statistics and owners
            val vehicleWithStats = vehicleDomainServiceDeprecated.getVehicleWithStatistics(vehicle.id)!!
            val owners = associationService.getVehicleOwners(vehicle.id)

            return VehicleDetailResponse.from(vehicleWithStats, owners)
        } catch (e: DomainException) {
            logger.error("Failed to create vehicle: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error creating vehicle", e)
            throw RuntimeException("Failed to create vehicle", e)
        }
    }

    fun updateVehicle(id: Long, request: UpdateVehicleRequest): VehicleDetailResponse {
        logger.info("Updating vehicle with ID: $id")

        try {
            validateUpdateVehicleRequest(request)

            val command = UpdateVehicleCommand(
                make = request.make,
                model = request.model,
                year = request.year ?: 2024, // Default year if null
                licensePlate = request.licensePlate,
                color = request.color,
                vin = request.vin,
                mileage = request.mileage,
                ownersIds = request.ownerIds.map { it.toLong() }
            )

            val vehicle = vehicleDomainServiceDeprecated.updateVehicle(VehicleId.of(id), command)

            logger.info("Successfully updated vehicle with ID: $id")

            // Get vehicle with statistics and owners
            val vehicleWithStats = vehicleDomainServiceDeprecated.getVehicleWithStatistics(vehicle.id)!!
            val owners = associationService.getVehicleOwners(vehicle.id)

            return VehicleDetailResponse.from(vehicleWithStats, owners)
        } catch (e: DomainException) {
            logger.error("Failed to update vehicle $id: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error updating vehicle $id", e)
            throw RuntimeException("Failed to update vehicle", e)
        }
    }
    
    fun updateVehicleLastVisit(id: Long, companyId: Long, date: LocalDateTime) {
        vehicleDomainServiceDeprecated.updateVehicleLastVisit(id, companyId, date)
    }

    fun updateVehicleStatistics(id: Long, gmv: BigDecimal = BigDecimal.ZERO, counter: Long = 0L) {
        logger.debug("Updating statistics for vehicle ID: $id")

        try {
            vehicleDomainServiceDeprecated.updateStatistics(VehicleId.of(id), gmv, counter)
            logger.info("Successfully updated statistics for vehicle ID: $id")
        } catch (e: DomainException) {
            logger.error("Failed to update statistics for vehicle $id: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error updating statistics for vehicle $id", e)
            throw RuntimeException("Failed to update vehicle statistics", e)
        }
    }

    @Transactional(readOnly = true)
    fun getVehicleById(id: Long): VehicleDetailResponse? {
        logger.debug("Getting vehicle by ID: $id")

        val vehicleWithStats = vehicleDomainServiceDeprecated.getVehicleWithStatistics(VehicleId.of(id))
            ?: return null

        val owners = associationService.getVehicleOwners(VehicleId.of(id))

        return VehicleDetailResponse.from(vehicleWithStats, owners)
    }

    @Transactional(readOnly = true)
    fun searchVehicles(
        make: String? = null,
        model: String? = null,
        licensePlate: String? = null,
        vin: String? = null,
        year: Int? = null,
        pageable: Pageable
    ): Page<VehicleDetailResponse> {
        logger.debug("Searching vehicles with criteria")

        val criteria = VehicleSearchCriteria(
            make = make,
            model = model,
            licensePlate = licensePlate,
            vin = vin,
            year = year
        )

        return vehicleDomainServiceDeprecated.searchVehicles(criteria, pageable)
            .map { vehicle ->
                val vehicleWithStats = vehicleDomainServiceDeprecated.getVehicleWithStatistics(vehicle.id)!!
                val owners = associationService.getVehicleOwners(vehicle.id)
                VehicleDetailResponse.from(vehicleWithStats, owners)
            }
    }

    fun deleteVehicle(id: Long): Boolean {
        logger.info("Deleting vehicle with ID: $id")

        val deleted = vehicleDomainServiceDeprecated.deleteVehicle(VehicleId.of(id))

        if (deleted) {
            logger.info("Successfully deleted vehicle with ID: $id")
        } else {
            logger.warn("Vehicle with ID: $id not found for deletion")
        }

        return deleted
    }

    fun addOwnerToVehicle(vehicleId: Long, clientId: Long): Boolean {
        logger.info("Adding owner $clientId to vehicle $vehicleId")

        try {
            associationService.associateClientWithVehicle(
                ClientId.of(clientId),
                VehicleId.of(vehicleId),
                VehicleRelationshipType.OWNER
            )
            return true
        } catch (e: DomainException) {
            logger.error("Failed to add owner to vehicle: ${e.message}")
            throw e
        }
    }

    fun removeOwnerFromVehicle(vehicleId: Long, clientId: Long): Boolean {
        logger.info("Removing owner $clientId from vehicle $vehicleId")

        return associationService.removeAssociation(
            ClientId.of(clientId),
            VehicleId.of(vehicleId)
        )
    }

    private fun validateCreateVehicleRequest(request: CreateVehicleRequest) {
        require(request.make.isNotBlank()) { "Make cannot be blank" }
        require(request.model.isNotBlank()) { "Model cannot be blank" }
        require(request.licensePlate.isNotBlank()) { "License plate cannot be blank" }
    }

    private fun validateUpdateVehicleRequest(request: UpdateVehicleRequest) {
        require(request.make.isNotBlank()) { "Make cannot be blank" }
        require(request.model.isNotBlank()) { "Model cannot be blank" }
        require(request.licensePlate.isNotBlank()) { "License plate cannot be blank" }
        request.year?.let { year ->
            require(year in 1900..2100) { "Year must be between 1900 and 2100" }
        }
    }
}