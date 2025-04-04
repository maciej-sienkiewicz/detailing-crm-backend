package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.ServiceHistory
import com.carslab.crm.domain.model.ServiceHistoryId
import com.carslab.crm.domain.model.VehicleId
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "service_history")
class ServiceHistoryEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    var vehicle: VehicleEntity,

    @Column(nullable = false)
    var date: LocalDate,

    @Column(name = "service_type", nullable = false)
    var serviceType: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(nullable = false)
    var price: Double,

    @Column(name = "protocol_id", nullable = true)
    var protocolId: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): ServiceHistory {
        return ServiceHistory(
            id = ServiceHistoryId(id),
            vehicleId = VehicleId(vehicle.id),
            date = date,
            serviceType = serviceType,
            description = description,
            price = price,
            protocolId = protocolId,
            audit = Audit(
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        )
    }

    companion object {
        fun fromDomain(domain: ServiceHistory, vehicleEntity: VehicleEntity): ServiceHistoryEntity {
            return ServiceHistoryEntity(
                id = domain.id.value,
                vehicle = vehicleEntity,
                date = domain.date,
                serviceType = domain.serviceType,
                description = domain.description,
                price = domain.price,
                protocolId = domain.protocolId,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}