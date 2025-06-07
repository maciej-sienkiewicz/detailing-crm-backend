package com.carslab.crm.clients.infrastructure.persistence.entity

import com.carslab.crm.clients.domain.model.CreateVehicle
import com.carslab.crm.clients.domain.model.Vehicle
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.clients.domain.model.VehicleServiceInfo
import com.carslab.crm.clients.domain.model.shared.AuditInfo
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.math.BigDecimal
import java.time.LocalDateTime


@Entity
@Table(
    name = "vehicles",
    indexes = [
        Index(name = "idx_vehicle_license_plate", columnList = "license_plate"),
        Index(name = "idx_vehicle_vin", columnList = "vin"),
        Index(name = "idx_vehicle_company_id", columnList = "company_id"),
        Index(name = "idx_vehicle_make_model", columnList = "make, model")
    ]
)
class VehicleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "make", nullable = false, length = 100)
    var make: String,

    @Column(name = "model", nullable = false, length = 100)
    var model: String,

    @Column(name = "year")
    var year: Int? = null,

    @Column(name = "license_plate", nullable = false, length = 20)
    var licensePlate: String,

    @Column(name = "color", length = 50)
    var color: String? = null,

    @Column(name = "vin", length = 17)
    var vin: String? = null,

    @Column(name = "mileage")
    var mileage: Long? = null,

    @Column(name = "total_services", nullable = false)
    var totalServices: Int = 0,

    @Column(name = "last_service_date")
    var lastServiceDate: LocalDateTime? = null,

    @Column(name = "total_spent", nullable = false, precision = 10, scale = 2)
    var totalSpent: BigDecimal = BigDecimal.ZERO,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null,

    @Column(name = "updated_by", length = 100)
    var updatedBy: String? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Column(name = "active", nullable = false)
    var active: Boolean = true
) {

    @OneToMany(
        mappedBy = "vehicle",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.REMOVE]
    )
    @BatchSize(size = 50)
    var associations: MutableSet<ClientVehicleAssociationEntity> = mutableSetOf()

    @OneToOne(
        mappedBy = "vehicle",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var statistics: VehicleStatisticsEntity? = null

    fun toDomain(): Vehicle = Vehicle(
        id = VehicleId.of(id!!),
        make = make,
        model = model,
        year = year,
        licensePlate = licensePlate,
        color = color,
        vin = vin,
        mileage = mileage,
        serviceInfo = VehicleServiceInfo(
            totalServices = totalServices,
            lastServiceDate = lastServiceDate,
            totalSpent = totalSpent
        ),
        audit = AuditInfo(
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdBy = createdBy,
            updatedBy = updatedBy,
            version = version
        )
    )

    companion object {
        fun fromDomain(vehicle: CreateVehicle, companyId: Long): VehicleEntity = VehicleEntity(
            id = null,
            companyId = companyId,
            make = vehicle.make,
            model = vehicle.model,
            year = vehicle.year,
            licensePlate = vehicle.licensePlate,
            color = vehicle.color,
            vin = vehicle.vin,
            mileage = vehicle.mileage,
            totalServices = vehicle.serviceInfo.totalServices,
            lastServiceDate = vehicle.serviceInfo.lastServiceDate,
            totalSpent = vehicle.serviceInfo.totalSpent,
            createdAt = vehicle.audit.createdAt,
            updatedAt = vehicle.audit.updatedAt,
            createdBy = vehicle.audit.createdBy,
            updatedBy = vehicle.audit.updatedBy,
            version = vehicle.audit.version
        )
    }
}