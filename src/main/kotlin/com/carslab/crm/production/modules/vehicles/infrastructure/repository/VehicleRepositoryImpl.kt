package com.carslab.crm.production.modules.vehicles.infrastructure.repository

import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleRepository
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleSearchCriteria
import com.carslab.crm.production.modules.vehicles.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.vehicles.infrastructure.mapper.toEntity
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class VehicleRepositoryImpl(
    private val jpaRepository: VehicleJpaRepository
) : VehicleRepository {

    private val logger = LoggerFactory.getLogger(VehicleRepositoryImpl::class.java)

    override fun save(vehicle: Vehicle): Vehicle {
        logger.debug("Saving vehicle: {} for company: {}", vehicle.id.value, vehicle.companyId)

        val entity = vehicle.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Vehicle saved: {}", savedEntity.id)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: VehicleId): Vehicle? {
        logger.debug("Finding vehicle by ID: {}", id.value)

        val result = jpaRepository.findById(id.value)
            .filter { it.active }
            .map { it.toDomain() }
            .orElse(null)

        if (result == null) {
            logger.debug("Vehicle not found: {}", id.value)
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Vehicle> {
        logger.debug("Finding vehicles for company: {}", companyId)

        val entities = jpaRepository.findByCompanyIdAndActiveTrue(companyId, pageable)
        val vehicles = entities.map { it.toDomain() }

        logger.debug("Found {} vehicles for company: {}", vehicles.numberOfElements, companyId)
        return vehicles
    }

    @Transactional(readOnly = true)
    override fun findByLicensePlate(licensePlate: String, companyId: Long): Vehicle? {
        logger.debug("Finding vehicle by license plate: {} for company: {}", licensePlate, companyId)

        return jpaRepository.findByLicensePlateAndCompanyIdAndActiveTrue(licensePlate, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByVin(vin: String, companyId: Long): Vehicle? {
        logger.debug("Finding vehicle by VIN: {} for company: {}", vin, companyId)

        return jpaRepository.findByVinAndCompanyIdAndActiveTrue(vin, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun existsByLicensePlate(licensePlate: String, companyId: Long): Boolean {
        return jpaRepository.existsByLicensePlateAndCompanyIdAndActiveTrue(licensePlate, companyId)
    }

    @Transactional(readOnly = true)
    override fun existsByVin(vin: String, companyId: Long): Boolean {
        return jpaRepository.existsByVinAndCompanyIdAndActiveTrue(vin, companyId)
    }

    override fun deleteById(id: VehicleId): Boolean {
        logger.debug("Soft deleting vehicle: {}", id.value)

        return try {
            if (jpaRepository.existsById(id.value)) {
                jpaRepository.softDeleteByIdAndCompanyId(id.value, 0L, LocalDateTime.now())
                logger.debug("Vehicle soft deleted: {}", id.value)
                true
            } else {
                logger.debug("Vehicle not found for deletion: {}", id.value)
                false
            }
        } catch (e: Exception) {
            logger.error("Error deleting vehicle: {}", id.value, e)
            false
        }
    }

    @Transactional(readOnly = true)
    override fun searchVehicles(
        companyId: Long,
        searchCriteria: VehicleSearchCriteria,
        pageable: Pageable
    ): Page<Vehicle> {
        logger.debug("Searching vehicles for company: {} with criteria", companyId)

        val offset = pageable.pageNumber * pageable.pageSize
        val limit = pageable.pageSize

        val entities = jpaRepository.searchVehicles(
            companyId = companyId,
            make = searchCriteria.make,
            model = searchCriteria.model,
            licensePlate = searchCriteria.licensePlate,
            vin = searchCriteria.vin,
            year = searchCriteria.year,
            ownerName = searchCriteria.ownerName,
            minVisits = searchCriteria.minVisits,
            maxVisits = searchCriteria.maxVisits,
            limit = limit,
            offset = offset
        )

        val totalCount = jpaRepository.countSearchVehicles(
            companyId = companyId,
            make = searchCriteria.make,
            model = searchCriteria.model,
            licensePlate = searchCriteria.licensePlate,
            vin = searchCriteria.vin,
            year = searchCriteria.year,
            ownerName = searchCriteria.ownerName,
            minVisits = searchCriteria.minVisits,
            maxVisits = searchCriteria.maxVisits
        )

        val vehicles = entities.map { it.toDomain() }
        logger.debug("Found {} vehicles matching criteria for company: {}", vehicles.size, companyId)

        return PageImpl(vehicles, pageable, totalCount)
    }

    override fun existsByIdAndCompanyId(
        vehicleId: VehicleId,
        companyId: Long
    ): Boolean = jpaRepository.existsByIdAndCompanyIdAndActiveTrue(vehicleId.value, companyId)
}