package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.model.stats.VehicleStats
import com.carslab.crm.domain.port.ClientStatisticsRepository
import com.carslab.crm.domain.port.VehicleStatisticsRepository
import com.carslab.crm.infrastructure.persistence.entity.ClientStatisticsEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.entity.VehicleStatisticsEntity
import com.carslab.crm.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ClientStatisticsJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.VehicleJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.VehicleStatisticsJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import java.math.BigDecimal


@Repository
class JpaClientStatisticsRepositoryAdapter(
    private val clientStatisticsJpaRepository: ClientStatisticsJpaRepository,
    private val clientJpaRepository: ClientJpaRepository
) : ClientStatisticsRepository {

    override fun save(client: ClientStats): ClientStats {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val clientEntity = clientJpaRepository.findByCompanyIdAndId(companyId, client.clientId)
            .orElse(null) ?: throw IllegalStateException("Client not found or access denied")

        val entity = if (clientStatisticsJpaRepository.existsById(client.clientId)) {
            val existingEntity = clientStatisticsJpaRepository.findByClientIdAndCompanyId(client.clientId, companyId).get()

            if (existingEntity.client?.companyId != companyId) {
                throw IllegalArgumentException("Access denied to statistics for this client")
            }

            existingEntity.visitNo = client.visitNo
            existingEntity.gmv = client.gmv
            existingEntity.vehiclesNo = client.vehiclesNo
            existingEntity
        } else {
            ClientStatisticsEntity.fromDomain(client)
        }

        val savedEntity = clientStatisticsJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: ClientId): ClientStats? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val clientEntity = clientJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .orElse(null) ?: return null

        return clientStatisticsJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }
}

@Repository
class JpaVehicleStatisticsRepositoryAdapter(
    private val vehicleStatisticsJpaRepository: VehicleStatisticsJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository
) : VehicleStatisticsRepository {

    override fun save(vehicleStats: VehicleStats): VehicleStats {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // Verify vehicle belongs to current company
        val vehicleEntity = vehicleJpaRepository.findByCompanyIdAndId(companyId, vehicleStats.vehicleId)
            .orElse(null) ?: throw IllegalStateException("Vehicle not found or access denied")

        val entity = if (vehicleStatisticsJpaRepository.existsById(vehicleStats.vehicleId)) {
            val existingEntity = vehicleStatisticsJpaRepository.findByVehicleIdAndCompanyId(vehicleStats.vehicleId, companyId).get()

            // Verify the vehicle of the statistics belongs to current company
            if (existingEntity.vehicle?.companyId != companyId) {
                throw IllegalArgumentException("Access denied to statistics for this vehicle")
            }

            existingEntity.visitNo = vehicleStats.visitNo
            existingEntity.gmv = vehicleStats.gmv
            existingEntity
        } else {
            VehicleStatisticsEntity.fromDomain(vehicleStats)
        }

        val savedEntity = vehicleStatisticsJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: VehicleId): VehicleStats {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // First check if vehicle exists and belongs to company
        val vehicleEntity = vehicleJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .orElse(null) ?: return VehicleStats(id.value, 0, BigDecimal.ZERO)

        return vehicleStatisticsJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(VehicleStats(id.value, 0, BigDecimal.ZERO))
    }
}