package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.Vehicle
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.port.ClientRepository
import com.carslab.crm.domain.port.VehicleRepository
import com.carslab.crm.infrastructure.persistence.entity.ClientEntity
import com.carslab.crm.infrastructure.persistence.entity.VehicleEntity
import com.carslab.crm.infrastructure.persistence.repository.VehicleJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaVehicleRepositoryAdapter(
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val clientRepository: ClientRepository) : VehicleRepository {

    override fun save(vehicle: Vehicle): Vehicle {
        val entity = if (vehicle.id.value > 0) {
            vehicleJpaRepository.flush()
            val existingEntity = vehicleJpaRepository.findById(vehicle.id.value).orElse(null)
                ?: VehicleEntity.fromDomain(vehicle)

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
        return vehicleJpaRepository.findAll().map { it.toDomain() }
    }

    override fun findById(id: VehicleId): Vehicle? {
        return vehicleJpaRepository.findById(id.value).map { it.toDomain() }.orElse(null)
    }

    override fun findByIds(ids: List<VehicleId>): List<Vehicle> {
        return vehicleJpaRepository.findAllById(ids.map { it.value }).map { it.toDomain() }
    }

    override fun deleteById(id: VehicleId): Boolean {
        return if (vehicleJpaRepository.existsById(id.value)) {
            vehicleJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    override fun findByVinOrLicensePlate(vin: String?, licensePlate: String?): Vehicle? {
        return vehicleJpaRepository.findByVinOrLicensePlate(vin, licensePlate)?.toDomain()
    }

    override fun findByClientId(clientId: ClientId): List<Vehicle> {
        return vehicleJpaRepository.findAllByClientId(clientId.value).map { it.toDomain() }
            .toList()
    }
}