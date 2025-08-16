package com.carslab.crm.modules.clients.infrastructure.persistence.adapter

import com.carslab.crm.modules.clients.domain.model.CreateVehicle
import com.carslab.crm.modules.clients.domain.model.Vehicle
import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.modules.clients.domain.port.VehicleRepositoryDeprecated
import com.carslab.crm.modules.clients.domain.port.VehicleSearchCriteria
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.VehicleEntityDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleJpaRepositoryDeprecated
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class VehicleRepositoryDeprecatedAdapter(
    private val vehicleJpaRepositoryDeprecated: VehicleJpaRepositoryDeprecated,
    private val securityContext: SecurityContext
) : VehicleRepositoryDeprecated {

    override fun save(vehicle: Vehicle): Vehicle {
        val companyId = securityContext.getCurrentCompanyId()
        val existing = vehicleJpaRepositoryDeprecated.findByIdAndCompanyId(vehicle.id.value, companyId)
            .orElseThrow { IllegalArgumentException("Vehicle not found or access denied") }
        updateEntityFromDomain(existing, vehicle)
        val entity = existing

        val saved = vehicleJpaRepositoryDeprecated.save(entity)
        return saved.toDomain()
    }

    override fun save(vehicle: CreateVehicle): Vehicle {
        val companyId = securityContext.getCurrentCompanyId()
        val entity = VehicleEntityDeprecated.fromDomain(vehicle, companyId)
        val saved = vehicleJpaRepositoryDeprecated.save(entity)
        return saved.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: VehicleId): Vehicle? {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepositoryDeprecated.findByIdAndCompanyId(id.value, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<VehicleId>): List<Vehicle> {
        val companyId = securityContext.getCurrentCompanyId()
        return ids.mapNotNull { id ->
            vehicleJpaRepositoryDeprecated.findByIdAndCompanyId(id.value, companyId)
                .map { it.toDomain() }
                .orElse(null)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<Vehicle> {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepositoryDeprecated.findByCompanyId(companyId, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByLicensePlate(licensePlate: String): Vehicle? {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepositoryDeprecated.findByLicensePlateAndCompanyId(licensePlate, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByVin(vin: String): Vehicle? {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepositoryDeprecated.findByVinAndCompanyId(vin, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByVinOrLicensePlate(vin: String?, licensePlate: String?): Vehicle? {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepositoryDeprecated.findByVinOrLicensePlateAndCompanyId(vin, licensePlate, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun searchVehicles(criteria: VehicleSearchCriteria, pageable: Pageable): Page<Vehicle> {
        val companyId = securityContext.getCurrentCompanyId()

        // Używamy native query zamiast problematycznego JPQL
        val offset = pageable.pageNumber * pageable.pageSize
        val limit = pageable.pageSize

        val vehicles = vehicleJpaRepositoryDeprecated.searchVehiclesNative(
            criteria.make,
            criteria.model,
            criteria.licensePlate,
            criteria.vin,
            criteria.year,
            companyId,
            limit,
            offset
        ).map { it.toDomain() }

        val total = vehicleJpaRepositoryDeprecated.countSearchVehicles(
            criteria.make,
            criteria.model,
            criteria.licensePlate,
            criteria.vin,
            criteria.year,
            companyId
        )

        return PageImpl(vehicles, pageable, total)
    }

    @Transactional(readOnly = true)
    override fun existsById(id: VehicleId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepositoryDeprecated.findByIdAndCompanyId(id.value, companyId).isPresent
    }

    override fun deleteById(id: VehicleId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        val deleted = vehicleJpaRepositoryDeprecated.softDeleteByIdAndCompanyId(id.value, companyId, LocalDateTime.now())
        return deleted > 0
    }

    @Transactional(readOnly = true)
    override fun count(): Long {
        val companyId = securityContext.getCurrentCompanyId()
        return vehicleJpaRepositoryDeprecated.countByCompanyId(companyId)
    }

    private fun updateEntityFromDomain(entity: VehicleEntityDeprecated, domain: Vehicle) {
        entity.make = domain.make
        entity.model = domain.model
        entity.year = domain.year
        entity.licensePlate = domain.licensePlate
        entity.vin = domain.vin
        entity.color = domain.color
        entity.mileage = domain.mileage
        entity.updatedAt = domain.audit.updatedAt
        entity.updatedBy = domain.audit.updatedBy
    }
}