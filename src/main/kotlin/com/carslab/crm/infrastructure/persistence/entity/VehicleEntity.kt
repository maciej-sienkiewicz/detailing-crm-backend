package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.Vehicle
import com.carslab.crm.domain.model.VehicleId
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "vehicles")
class VehicleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var make: String,

    @Column(nullable = false)
    var model: String,

    @Column(nullable = true)
    var year: Int? = null,

    @Column(name = "license_plate", nullable = false)
    var licensePlate: String,

    @Column(nullable = true)
    var color: String? = null,

    @Column(nullable = true)
    var vin: String? = null,

    @Column(nullable = true)
    var mileage: Long? = null,

    @Column(name = "total_services")
    var totalServices: Int = 0,

    @Column(name = "last_service_date")
    var lastServiceDate: LocalDateTime? = null,

    @Column(name = "total_spent")
    var totalSpent: Double = 0.0,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Version
    var version: Long? = 0,

    @ManyToMany
    @JoinTable(
        name = "client_vehicles",
        joinColumns = [JoinColumn(name = "vehicle_id")],
        inverseJoinColumns = [JoinColumn(name = "client_id")]
    )
    var owners: MutableSet<ClientEntity> = mutableSetOf()

) {
    fun toDomain(): Vehicle = Vehicle(
        id = VehicleId(id!!),
        make = make,
        model = model,
        year = year,
        licensePlate = licensePlate,
        color = color,
        vin = vin,
        totalServices = totalServices,
        lastServiceDate = lastServiceDate,
        totalSpent = totalSpent,
        mileage = mileage,
        ownerIds = owners.map { it.id!! }.toSet(),
        audit = Audit(
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    )

    companion object {
        fun fromDomain(domain: Vehicle): VehicleEntity {
            return VehicleEntity(
                make = domain.make ?: "",
                model = domain.model ?: "",
                year = domain.year,
                licensePlate = domain.licensePlate ?: "",
                color = domain.color,
                vin = domain.vin,
                mileage = domain.mileage,
                totalServices = domain.totalServices,
                lastServiceDate = domain.lastServiceDate,
                totalSpent = domain.totalSpent,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}