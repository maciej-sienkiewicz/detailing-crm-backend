package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.model.stats.VehicleStats
import com.carslab.crm.domain.port.ClientStatisticsRepository
import com.carslab.crm.domain.port.VehicleStatisticsRepository
import com.carslab.crm.infrastructure.persistence.entity.ClientStatisticsEntity
import com.carslab.crm.infrastructure.persistence.entity.VehicleStatisticsEntity
import com.carslab.crm.infrastructure.persistence.repository.ClientStatisticsJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.VehicleStatisticsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Repository
class JpaClientStatisticsRepositoryAdapter(
    private val clientStatisticsJpaRepository: ClientStatisticsJpaRepository
) : ClientStatisticsRepository {

    override fun save(client: ClientStats): ClientStats {
        val entity = if (clientStatisticsJpaRepository.existsById(client.clientId)) {
            val existingEntity = clientStatisticsJpaRepository.findById(client.clientId).get()
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
        return clientStatisticsJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }
}

@Repository
class JpaVehicleStatisticsRepositoryAdapter(
    private val vehicleStatisticsJpaRepository: VehicleStatisticsJpaRepository
) : VehicleStatisticsRepository {

    override fun save(vehicleStats: VehicleStats): VehicleStats {
        val entity = if (vehicleStatisticsJpaRepository.existsById(vehicleStats.vehicleId)) {
            val existingEntity = vehicleStatisticsJpaRepository.findById(vehicleStats.vehicleId).get()
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
        return vehicleStatisticsJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(VehicleStats(id.value, 0, BigDecimal.ZERO))
    }
}