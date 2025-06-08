package com.carslab.crm.modules.clients.infrastructure.persistence.entity

import com.carslab.crm.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.modules.clients.domain.model.VehicleStatistics
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "client_statistics")
class ClientStatisticsEntity(
    @Id
    @Column(name = "client_id")
    val clientId: Long,

    @Column(name = "visit_count", nullable = false)
    var visitCount: Long = 0,

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    var totalRevenue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "vehicle_count", nullable = false)
    var vehicleCount: Long = 0,

    @Column(name = "last_visit_date")
    var lastVisitDate: LocalDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    var client: ClientEntity? = null
) {
    fun toDomain(): ClientStatistics {
        return ClientStatistics(
            clientId = clientId,
            visitCount = visitCount,
            totalRevenue = totalRevenue,
            vehicleCount = vehicleCount,
            lastVisitDate = lastVisitDate
        )
    }

    companion object {
        fun fromDomain(domain: ClientStatistics): ClientStatisticsEntity {
            return ClientStatisticsEntity(
                clientId = domain.clientId,
                visitCount = domain.visitCount,
                totalRevenue = domain.totalRevenue,
                vehicleCount = domain.vehicleCount,
                lastVisitDate = domain.lastVisitDate
            )
        }
    }
}

@Entity
@Table(name = "vehicle_statistics")
class VehicleStatisticsEntity(
    @Id
    @Column(name = "vehicle_id")
    val vehicleId: Long,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", insertable = false, updatable = false)
    val vehicle: VehicleEntity? = null,

    @Column(name = "visit_count", nullable = false)
    var visitCount: Long = 0,

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    var totalRevenue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "last_visit_date")
    var lastVisitDate: LocalDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): VehicleStatistics = VehicleStatistics(
        vehicleId = vehicleId,
        visitCount = visitCount,
        totalRevenue = totalRevenue,
        lastVisitDate = lastVisitDate
    )

    companion object {
        fun fromDomain(statistics: VehicleStatistics): VehicleStatisticsEntity = VehicleStatisticsEntity(
            vehicleId = statistics.vehicleId,
            visitCount = statistics.visitCount,
            totalRevenue = statistics.totalRevenue,
            lastVisitDate = statistics.lastVisitDate
        )
    }
}