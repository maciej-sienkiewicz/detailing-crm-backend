package com.carslab.crm.production.modules.reservations.infrastructure.entity

import com.carslab.crm.production.modules.reservations.domain.models.aggregates.Reservation
import com.carslab.crm.production.modules.reservations.domain.models.enums.ReservationStatus
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationId
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationPeriod
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.VehicleBasicInfo
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "reservations",
    indexes = [
        Index(name = "idx_reservations_company_id", columnList = "companyId"),
        Index(name = "idx_reservations_status", columnList = "status"),
        Index(name = "idx_reservations_start_date", columnList = "startDate"),
        Index(name = "idx_reservations_contact_phone", columnList = "contactPhone")
    ]
)
data class ReservationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 200)
    val title: String,

    @Column(nullable = false, length = 20)
    val contactPhone: String,

    @Column(length = 100)
    val contactName: String?,

    @Column(nullable = false, length = 100)
    val vehicleMake: String,

    @Column(nullable = false, length = 100)
    val vehicleModel: String,

    @Column(nullable = false)
    val startDate: LocalDateTime,

    @Column(nullable = false)
    val endDate: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val status: ReservationStatus,

    @Column(length = 1000)
    val notes: String?,

    @Column(nullable = false, length = 50)
    val calendarColorId: String,

    @Column
    val visitId: Long?,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "reservationId", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val services: MutableList<ReservationServiceEntity> = mutableListOf()
) {
    fun toDomain(): Reservation {
        return Reservation(
            id = id?.let { ReservationId.of(it) },
            companyId = companyId,
            title = title,
            contactPhone = contactPhone,
            contactName = contactName,
            vehicleInfo = VehicleBasicInfo(vehicleMake, vehicleModel),
            period = ReservationPeriod(startDate, endDate),
            status = status,
            services = services.map { it.toDomain() },
            notes = notes,
            calendarColorId = calendarColorId,
            visitId = visitId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(reservation: Reservation): ReservationEntity {
            return ReservationEntity(
                id = reservation.id?.value,
                companyId = reservation.companyId,
                title = reservation.title,
                contactPhone = reservation.contactPhone,
                contactName = reservation.contactName,
                vehicleMake = reservation.vehicleInfo.make,
                vehicleModel = reservation.vehicleInfo.model,
                startDate = reservation.period.startDate,
                endDate = reservation.period.endDate,
                status = reservation.status,
                notes = reservation.notes,
                calendarColorId = reservation.calendarColorId,
                visitId = reservation.visitId,
                createdAt = reservation.createdAt,
                updatedAt = reservation.updatedAt
            )
        }
    }
}