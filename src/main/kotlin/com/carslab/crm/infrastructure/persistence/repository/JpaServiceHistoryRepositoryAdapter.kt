package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ServiceHistory
import com.carslab.crm.domain.model.ServiceHistoryId
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.port.ServiceHistoryRepository
import com.carslab.crm.domain.port.VehicleRepository
import com.carslab.crm.infrastructure.persistence.entity.ServiceHistoryEntity
import com.carslab.crm.infrastructure.persistence.repository.ServiceHistoryJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaServiceHistoryRepositoryAdapter(
    private val serviceHistoryJpaRepository: ServiceHistoryJpaRepository,
    private val vehicleJpaRepository: VehicleRepository
) : ServiceHistoryRepository {

    override fun save(serviceHistory: ServiceHistory): ServiceHistory {
        // Sprawdź, czy pojazd istnieje
        if (vehicleJpaRepository.findById(serviceHistory.vehicleId) != null) {
            throw IllegalStateException("Vehicle with ID ${serviceHistory.vehicleId.value} not found")
        }

        val entity = if (serviceHistoryJpaRepository.existsById(serviceHistory.id.value)) {
            val existingEntity = serviceHistoryJpaRepository.findById(serviceHistory.id.value).get()

            existingEntity.date = serviceHistory.date
            existingEntity.serviceType = serviceHistory.serviceType
            existingEntity.description = serviceHistory.description
            existingEntity.price = serviceHistory.price
            existingEntity.protocolId = serviceHistory.protocolId
            existingEntity.updatedAt = serviceHistory.audit.updatedAt

            existingEntity
        } else {
            ServiceHistoryEntity.fromDomain(serviceHistory)
        }

        val savedEntity = serviceHistoryJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findAll(): List<ServiceHistory> {
        return serviceHistoryJpaRepository.findAll().map { it.toDomain() }
    }

    override fun findById(id: ServiceHistoryId): ServiceHistory? {
        return serviceHistoryJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByVehicleId(vehicleId: VehicleId): List<ServiceHistory> {
        // Używamy bezpośrednio metody findByVehicleId zamiast findByVehicle_Id
        return serviceHistoryJpaRepository.findByVehicleId(vehicleId.value)
            .map { it.toDomain() }
    }

    override fun deleteById(id: ServiceHistoryId): Boolean {
        return if (serviceHistoryJpaRepository.existsById(id.value)) {
            serviceHistoryJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }
}