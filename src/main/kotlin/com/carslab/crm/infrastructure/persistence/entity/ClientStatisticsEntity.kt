package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.model.stats.VehicleStats
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "client_statistics")
class ClientStatisticsEntity(
    @Id
    @Column(name = "client_id")
    val clientId: Long,

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
            clientId = clientId,
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

    @Column(name = "visit_no")
    var visitNo: Long = 0,

    @Column
    var gmv: BigDecimal = BigDecimal.ZERO,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", insertable = false, updatable = false)
    var vehicle: VehicleEntity? = null
) {
    fun toDomain(): VehicleStats {
        return VehicleStats(
            vehicleId = vehicleId,
            visitNo = visitNo,
            gmv = gmv
        )
    }

    companion object {
        fun fromDomain(domain: VehicleStats): VehicleStatisticsEntity {
            return VehicleStatisticsEntity(
                vehicleId = domain.vehicleId,
                visitNo = domain.visitNo,
                gmv = domain.gmv
            )
        }
    }
}