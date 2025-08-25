package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.production.modules.vehicles.application.dto.CreateVehicleRequest
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.vehicles.application.service.VehicleCommandService
import com.carslab.crm.production.modules.vehicles.application.service.VehicleQueryService
import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class VisitVehicleResolver(
    private val vehicleQueryService: VehicleQueryService,
    private val vehicleCommandService: VehicleCommandService
) {
    private val logger = LoggerFactory.getLogger(VisitVehicleResolver::class.java)

    fun resolveVehicle(vehicleDetails: VehicleDetails): VehicleResponse {
        if (!vehicleDetails.vin.isNullOrBlank() || vehicleDetails.licensePlate.isNotBlank()) {
            findExistingVehicle(vehicleDetails.vin, vehicleDetails.licensePlate)?.let { existingVehicle ->
                logger.debug("Found existing vehicle: ${existingVehicle.licensePlate}")
                return existingVehicle
            }
        }

        logger.info("Creating new vehicle: ${vehicleDetails.make} ${vehicleDetails.model}")
        return createVehicle(vehicleDetails)
    }

    private fun findExistingVehicle(vin: String?, licensePlate: String?): VehicleResponse? {
        return try {
            vehicleQueryService.searchVehicles(
                make = null,
                model = null,
                licensePlate = licensePlate,
                vin = vin,
                year = null,
                ownerName = null,
                minVisits = null,
                maxVisits = null,
                pageable = PageRequest.of(0, 1)
            ).content.firstOrNull()?.let { tableResponse ->
                VehicleResponse(
                    id = tableResponse.id.toString(),
                    make = tableResponse.make,
                    model = tableResponse.model,
                    year = tableResponse.year,
                    licensePlate = tableResponse.licensePlate,
                    color = tableResponse.color,
                    vin = tableResponse.vin,
                    mileage = tableResponse.mileage,
                    displayName = "${tableResponse.make} ${tableResponse.model} (${tableResponse.licensePlate})",
                    createdAt = tableResponse.createdAt,
                    updatedAt = tableResponse.updatedAt
                )
            }
        } catch (e: Exception) {
            logger.warn("Error searching for vehicle", e)
            null
        }
    }

    private fun createVehicle(request: VehicleDetails): VehicleResponse {
        val createRequest = CreateVehicleRequest(
            make = request.make,
            model = request.model,
            year = request.productionYear,
            licensePlate = request.licensePlate,
            color = request.color,
            vin = request.vin,
            mileage = request.mileage,
            ownerIds = request.ownerId?.let { listOf(it) } ?: emptyList()
        )

        return try {
            vehicleCommandService.createVehicle(createRequest)
        } catch (e: Exception) {
            logger.warn("Failed to create vehicle, checking if it was created by another thread")
            findExistingVehicle(request.vin, request.licensePlate)
                ?: throw IllegalStateException("Could not create or find vehicle: ${request.licensePlate}", e)
        }
    }
}

data class VehicleDetails(
    val make: String,
    val model: String,
    val licensePlate: String,
    val productionYear: Int? = null,
    val vin: String? = null,
    val color: String? = null,
    val mileage: Long? = null,
    val ownerId: Long? = null
)