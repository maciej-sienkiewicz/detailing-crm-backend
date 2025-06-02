package com.carslab.crm.clients.infrastructure.persistence.adapter

import com.carslab.crm.clients.domain.model.Vehicle
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.clients.domain.port.VehicleRepository
import com.carslab.crm.clients.domain.port.VehicleSearchCriteria
import com.carslab.crm.clients.infrastructure.persistence.entity.VehicleEntity
import com.carslab.crm.clients.infrastructure.persistence.repository.VehicleJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class VehicleRepositoryAdapter(
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val securityContext: SecurityContext
) : VehicleRepository {

    override fun save(vehicle: Vehicle): Vehicle {
        val companyId = securityContext.getCurrentCompanyId()
        val entity = if (vehicle.id.value > 0) {
            val existing = vehicleJpaRepository.findByIdAndCompanyId(vehicle.id.value, companyId)
                .orElseThrow { IllegalArgumentException("Vehicle not found or access denied") }
            updateEntityFromDomain(existing, vehicle)
            existing
        } else {
            // Create new
            VehicleEntity.fromDomain(vehicle, companyId)
        }

        val saved = vehicleJpaRepository.save(entity)
        return saved.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: VehicleId): Vehicle? {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepository.findByIdAndCompanyId(id.value, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<VehicleId>): List<Vehicle> {
        val companyId = securityContext.getCurrentCompanyId()
        return ids.mapNotNull { id ->
            vehicleJpaRepository.findByIdAndCompanyId(id.value, companyId)
                .map { it.toDomain() }
                .orElse(null)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<Vehicle> {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepository.findByCompanyId(companyId, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByLicensePlate(licensePlate: String): Vehicle? {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepository.findByLicensePlateAndCompanyId(licensePlate, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByVin(vin: String): Vehicle? {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepository.findByVinAndCompanyId(vin, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByVinOrLicensePlate(vin: String?, licensePlate: String?): Vehicle? {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepository.findByVinOrLicensePlateAndCompanyId(vin, licensePlate, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun searchVehicles(criteria: VehicleSearchCriteria, pageable: Pageable): Page<Vehicle> {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepository.searchVehicles(
            criteria.make, criteria.model, criteria.licensePlate,
            criteria.vin, criteria.year, companyId, pageable
        ).map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun existsById(id: VehicleId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepository.findByIdAndCompanyId(id.value, companyId).isPresent
    }

    override fun deleteById(id: VehicleId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        val deleted = vehicleJpaRepository.softDeleteByIdAndCompanyId(id.value, companyId, LocalDateTime.now())
        return deleted > 0
    }

    @Transactional(readOnly = true)
    override fun count(): Long {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepository.countByCompanyId(companyId)
    }

    private fun updateEntityFromDomain(entity: VehicleEntity, domain: Vehicle) {
        entity.make = domain.make
        entity.model = domain.model
        entity.year = domain.year
        entity.licensePlate = domain.licensePlate
        entity.color = domain.color
        entity.vin = domain.vin
        entity.mileage = domain.mileage
        entity.totalServices = domain.serviceInfo.totalServices
        entity.lastServiceDate = domain.serviceInfo.lastServiceDate
        entity.totalSpent = domain.serviceInfo.totalSpent
        entity.updatedAt = domain.audit.updatedAt
        entity.updatedBy = domain.audit.updatedBy
    }
}