package com.carslab.crm.production.modules.vehicles.infrastructure.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "vehicles",
    indexes = [
        Index(name = "idx_vehicle_company_id", columnList = "company_id"),
        Index(name = "idx_vehicle_license_plate", columnList = "company_id,license_plate"),
        Index(name = "idx_vehicle_vin", columnList = "company_id,vin"),
        Index(name = "idx_vehicle_make_model", columnList = "company_id,make,model"),
        Index(name = "idx_vehicle_active", columnList = "company_id,active")
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

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null,

    @Column(name = "updated_by", length = 100)
    var updatedBy: String? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true
)