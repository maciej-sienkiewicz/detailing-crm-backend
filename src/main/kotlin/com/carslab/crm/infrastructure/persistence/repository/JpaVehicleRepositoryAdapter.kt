package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.Vehicle
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.port.ClientRepository
import com.carslab.crm.domain.port.VehicleRepository
import com.carslab.crm.infrastructure.persistence.entity.ClientEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.entity.VehicleEntity
import com.carslab.crm.infrastructure.persistence.repository.VehicleJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaVehicleRepositoryAdapter(
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val clientRepository: ClientRepository
) : VehicleRepository {

    override fun save(vehicle: Vehicle): Vehicle {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val entity = if (vehicle.id.value > 0) {
            vehicleJpaRepository.flush()
            val existingEntity = vehicleJpaRepository.findByCompanyIdAndId(companyId, vehicle.id.value)
                .orElse(null) ?: VehicleEntity.fromDomain(vehicle)

            existingEntity.make = vehicle.make!!
            existingEntity.model = vehicle.model!!
            existingEntity.year = vehicle.year
            existingEntity.licensePlate = vehicle.licensePlate!!
            existingEntity.color = vehicle.color
            existingEntity.vin = vehicle.vin

            existingEntity.owners = clientRepository.findByIds(vehicle.ownerIds.map { ClientId(it) })
                .map {
                    val client = ClientEntity.fromDomain(it)
                    client.id = it.id.value
                    client
                }.toMutableSet()

            existingEntity
        } else {
            VehicleEntity.fromDomain(vehicle)
        }

        val savedEntity: VehicleEntity = vehicleJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findAll(): List<Vehicle> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return vehicleJpaRepository.findByCompanyId(companyId).map { it.toDomain() }
    }

    override fun findById(id: VehicleId): Vehicle? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return vehicleJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByIds(ids: List<VehicleId>): List<Vehicle> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return ids.mapNotNull { id ->
            vehicleJpaRepository.findByCompanyIdAndId(companyId, id.value)
                .map { it.toDomain() }
                .orElse(null)
        }
    }

    override fun deleteById(id: VehicleId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val entity = vehicleJpaRepository.findByCompanyIdAndId(companyId, id.value).orElse(null) ?: return false
        vehicleJpaRepository.delete(entity)
        return true
    }

    override fun findByVinOrLicensePlate(vin: String?, licensePlate: String?): Vehicle? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return vehicleJpaRepository.findByVinOrLicensePlateAndCompanyId(vin, licensePlate, companyId)?.toDomain()
    }

    override fun findByClientId(clientId: ClientId): List<Vehicle> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return vehicleJpaRepository.findAllByClientIdAndCompanyId(clientId.value, companyId)
            .map { it.toDomain() }
    }
}