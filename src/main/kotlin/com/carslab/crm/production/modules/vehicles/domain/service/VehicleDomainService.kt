package com.carslab.crm.production.modules.vehicles.domain.service

import com.carslab.crm.production.modules.associations.application.service.AssociationQueryService
import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.companysettings.domain.model.CompanyId
import com.carslab.crm.production.modules.vehicles.domain.command.CreateVehicleCommand
import com.carslab.crm.production.modules.vehicles.domain.command.UpdateVehicleCommand
import com.carslab.crm.production.modules.vehicles.domain.model.*
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleRepository
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleSearchCriteria
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleStatisticsRepository
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VehicleDomainService(
    private val vehicleRepository: VehicleRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
    private val associationQueryService: AssociationQueryService,
    private val clientQueryService: ClientQueryService
) {
    private val logger = LoggerFactory.getLogger(VehicleDomainService::class.java)

    fun createVehicle(command: CreateVehicleCommand): Vehicle {
        logger.debug("Creating vehicle: {} {} for company: {}", command.make, command.model, command.companyId)

        validateVehicleUniqueness(command.licensePlate, command.vin, command.companyId)

        val vehicle = Vehicle(
            id = VehicleId(0),
            companyId = command.companyId,
            make = command.make.trim(),
            model = command.model.trim(),
            year = command.year,
            licensePlate = command.licensePlate.trim(),
            color = command.color?.trim(),
            vin = command.vin?.trim(),
            mileage = command.mileage,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 0
        )

        val savedVehicle = vehicleRepository.save(vehicle)
        initializeVehicleStatistics(savedVehicle.id)

        logger.info("Vehicle created: {} for company: {}", savedVehicle.id.value, command.companyId)
        return savedVehicle
    }

    fun updateVehicle(vehicleId: VehicleId, command: UpdateVehicleCommand, companyId: Long): Vehicle {
        logger.debug("Updating vehicle: {} for company: {}", vehicleId.value, companyId)

        val existingVehicle = getVehicleForCompany(vehicleId, companyId)

        if (command.licensePlate != existingVehicle.licensePlate || command.vin != existingVehicle.vin) {
            validateVehicleUniqueness(command.licensePlate, command.vin, companyId, excludeVehicleId = vehicleId)
        }

        val updatedVehicle = existingVehicle.update(
            make = command.make.trim(),
            model = command.model.trim(),
            year = command.year,
            licensePlate = command.licensePlate.trim(),
            color = command.color?.trim(),
            vin = command.vin?.trim(),
            mileage = command.mileage
        )

        val saved = vehicleRepository.save(updatedVehicle)
        logger.info("Vehicle updated: {} for company: {}", vehicleId.value, companyId)
        return saved
    }

    fun getVehicleForCompany(vehicleId: VehicleId, companyId: Long): Vehicle {
        val vehicle = vehicleRepository.findById(vehicleId)
            ?: throw BusinessException("Vehicle not found: ${vehicleId.value}")

        if (!vehicle.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to vehicle: ${vehicleId.value}")
        }

        return vehicle
    }

    fun getEnhancedVehicle(vehicleId: VehicleId, companyId: Long): EnhancedVehicle {
        val vehicle = getVehicleForCompany(vehicleId, companyId)
        val statistics = vehicleStatisticsRepository.findByVehicleId(vehicleId)
        val owners = getVehicleOwners(listOf(vehicleId))

        return EnhancedVehicle(
            vehicle = vehicle,
            statistics = statistics,
            owners = owners[vehicleId] ?: emptyList()
        )
    }
    
    fun searchVehicles(companyId: Long, searchCriteria: VehicleSearchCriteria, pageable: Pageable): Page<Vehicle> {
        logger.debug("Searching vehicles for company: {} with criteria", companyId)
        return vehicleRepository.searchVehicles(companyId, searchCriteria, pageable)
    }
    
    fun getVehicles(companyId: Long, searchCriteria: VehicleSearchCriteria, pageable: Pageable): Page<Vehicle> {
        logger.debug("Fetching vehicles for company: {} with search criteria", companyId)
        return vehicleRepository.searchVehicles(companyId, searchCriteria, pageable)
    }

    fun enhanceVehicles(vehicles: Page<Vehicle>): Page<EnhancedVehicle> {
        val vehicleIds: List<VehicleId> = vehicles.content.map { it.id }
        val vehicleOwners = getVehicleOwners(vehicleIds)
        val statisticsMap = getVehicleStatistics(vehicleIds)

        return vehicles.map { vehicle ->
            EnhancedVehicle(
                vehicle = vehicle,
                statistics = statisticsMap[vehicle.id],
                owners = vehicleOwners[vehicle.id] ?: emptyList()
            )
        }
    }

    fun deleteVehicle(vehicleId: VehicleId, companyId: Long): Boolean {
        logger.debug("Deleting vehicle: {} for company: {}", vehicleId.value, companyId)

        getVehicleForCompany(vehicleId, companyId)

        vehicleStatisticsRepository.deleteByVehicleId(vehicleId)
        val deleted = vehicleRepository.deleteById(vehicleId)

        if (deleted) {
            logger.info("Vehicle deleted: {} for company: {}", vehicleId.value, companyId)
        }

        return deleted
    }

    private fun getVehicleOwners(vehicleIds: List<VehicleId>): Map<VehicleId, List<VehicleOwner>> {
        val vehicleOwners = associationQueryService.getVehicleOwners(vehicleIds)
        val clientsMap = clientQueryService.findByIds(vehicleOwners.values.flatten())
            .associateBy { ClientId(it.id.toLong()) }

        return vehicleOwners.mapValues { (_, clientIds) ->
            clientIds.mapNotNull(clientsMap::get).map(::toVehicleOwner)
        }
    }

    private fun toVehicleOwner(client: ClientResponse) = VehicleOwner(
        id = client.id.toLong(),
        firstName = client.firstName,
        lastName = client.lastName,
        fullName = client.fullName,
        email = client.email,
        phone = client.phone
    )

    private fun getVehicleStatistics(vehicleIds: List<VehicleId>): Map<VehicleId, VehicleStatistics> {
        return vehicleStatisticsRepository.findByVehicleIds(vehicleIds).associateBy { it.vehicleId }
    }

    private fun validateVehicleUniqueness(
        licensePlate: String,
        vin: String?,
        companyId: Long,
        excludeVehicleId: VehicleId? = null
    ) {
        val existingByPlate = vehicleRepository.findByLicensePlate(licensePlate, companyId)
        if (existingByPlate != null && existingByPlate.id != excludeVehicleId) {
            throw BusinessException("Vehicle with license plate $licensePlate already exists")
        }

        if (!vin.isNullOrBlank()) {
            val existingByVin = vehicleRepository.findByVin(vin, companyId)
            if (existingByVin != null && existingByVin.id != excludeVehicleId) {
                throw BusinessException("Vehicle with VIN $vin already exists")
            }
        }
    }

    private fun initializeVehicleStatistics(vehicleId: VehicleId) {
        val statistics = VehicleStatistics(vehicleId = vehicleId)
        vehicleStatisticsRepository.save(statistics)
    }

    fun exists(vehicleId: VehicleId, companyId: Long): Boolean =
        vehicleRepository.existsByIdAndCompanyId(vehicleId, companyId)
}