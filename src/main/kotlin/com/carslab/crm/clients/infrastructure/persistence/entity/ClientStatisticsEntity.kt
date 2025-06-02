package com.carslab.crm.clients.infrastructure.persistence.entity

import com.carslab.crm.clients.domain.model.VehicleStatistics
import com.carslab.crm.domain.model.stats.ClientStats
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "client_statistics")
class ClientStatisticsEntity(
    @Id
    @Column(name = "client_id")
    val clientId: Long? = null,

    @Column(name = "visit_no")
    var visitNo: Long = 0,

    @Column
    var gmv: BigDecimal = BigDecimal.ZERO,

    @Column(name = "vehicles_no")
    var vehiclesNo: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    var client: ClientEntity? = null
) {
    fun toDomain(): ClientStats {
        return ClientStats(
            clientId = clientId!!,
            visitNo = visitNo,
            gmv = gmv,
            vehiclesNo = vehiclesNo
        )
    }

    companion object {
        fun fromDomain(domain: ClientStats): ClientStatisticsEntity {
            return ClientStatisticsEntity(
                clientId = domain.clientId,
                visitNo = domain.visitNo,
                gmv = domain.gmv,
                vehiclesNo = domain.vehiclesNo
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