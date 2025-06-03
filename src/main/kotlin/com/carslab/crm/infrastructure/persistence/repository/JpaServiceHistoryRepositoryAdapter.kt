package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ServiceHistory
import com.carslab.crm.domain.model.ServiceHistoryId
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.domain.port.ServiceHistoryRepository
import com.carslab.crm.clients.domain.port.VehicleRepository
import com.carslab.crm.infrastructure.persistence.entity.ServiceHistoryEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.ServiceHistoryJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository

@Repository
class JpaServiceHistoryRepositoryAdapter(
    private val serviceHistoryJpaRepository: ServiceHistoryJpaRepository,
    private val vehicleJpaRepository: VehicleRepository
) : ServiceHistoryRepository {

    override fun save(serviceHistory: ServiceHistory): ServiceHistory {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // Sprawdź, czy pojazd istnieje i należy do tej samej firmy
        val vehicle = vehicleJpaRepository.findById(serviceHistory.vehicleId)
        if (vehicle == null) {
            throw IllegalStateException("Vehicle with ID ${serviceHistory.vehicleId.value} not found")
        }

        val entity = if (serviceHistoryJpaRepository.existsById(serviceHistory.id.value)) {
            val existingEntity = serviceHistoryJpaRepository.findByCompanyIdAndId(companyId, serviceHistory.id.value)
                .orElse(null) ?: throw IllegalStateException("Service history not found or access denied")

            existingEntity.date = serviceHistory.date
            existingEntity.serviceType = serviceHistory.serviceType
            existingEntity.description = serviceHistory.description
            existingEntity.price = serviceHistory.price
            existingEntity.protocolId = serviceHistory.protocolId
            existingEntity.updatedAt = serviceHistory.audit.updatedAt

            existingEntity
        } else {
            val newEntity = ServiceHistoryEntity.fromDomain(serviceHistory)
            newEntity.companyId = companyId
            newEntity
        }

        val savedEntity = serviceHistoryJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findAll(): List<ServiceHistory> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return serviceHistoryJpaRepository.findByCompanyId(companyId)
            .map { it.toDomain() }
    }

    override fun findById(id: ServiceHistoryId): ServiceHistory? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return serviceHistoryJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByVehicleId(vehicleId: VehicleId): List<ServiceHistory> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return serviceHistoryJpaRepository.findByVehicleIdAndCompanyId(vehicleId.value, companyId)
            .map { it.toDomain() }
    }

    override fun deleteById(id: ServiceHistoryId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val entity = serviceHistoryJpaRepository.findByCompanyIdAndId(companyId, id.value).orElse(null) ?: return false
        serviceHistoryJpaRepository.delete(entity)
        return true
    }
}