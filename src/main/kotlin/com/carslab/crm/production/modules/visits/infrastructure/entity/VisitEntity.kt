package com.carslab.crm.production.modules.visits.infrastructure.entity

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.command.DeliveryPerson
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.enums.ReferralSource
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitDocuments
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitPeriod
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "visits",
    indexes = [
        Index(name = "idx_visits_company_id", columnList = "companyId"),
        Index(name = "idx_visits_client_id", columnList = "clientId"),
        Index(name = "idx_visits_vehicle_id", columnList = "vehicleId"),
        Index(name = "idx_visits_status", columnList = "status"),
        Index(name = "idx_visits_start_date", columnList = "startDate")
    ]
)
class VisitEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 200)
    val title: String,

    @Column(nullable = false)
    val clientId: Long,

    @Column(nullable = false)
    val vehicleId: Long,

    @Column(nullable = false)
    val startDate: LocalDateTime,

    @Column(nullable = false)
    val endDate: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val status: VisitStatus,

    @Column(length = 1000)
    val notes: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    val referralSource: ReferralSource? = null,

    @Column(length = 100)
    val appointmentId: String? = null,

    @Column(nullable = false, length = 50)
    val calendarColorId: String,

    @Column(nullable = false)
    val keysProvided: Boolean = false,

    @Column(nullable = false)
    val documentsProvided: Boolean = false,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = true)
    val deliveryPersonId: String?,
    
    @Column(nullable = true)
    val deliveryPersonName: String?,
    
    @Column(nullable = true)
    val deliveryPersonPhoneNumber: String?,

    @OneToMany(mappedBy = "visitId", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val services: MutableList<VisitServiceEntity> = mutableListOf()
) {
    fun toDomain(updatedServices: List<VisitServiceEntity>): Visit {
        return Visit(
            id = id?.let { VisitId.of(it) },
            companyId = companyId,
            title = title,
            clientId = ClientId.of(clientId),
            vehicleId = VehicleId.of(vehicleId),
            period = VisitPeriod(startDate, endDate),
            status = status,
            services = updatedServices.map { it.toDomain() },
            documents = VisitDocuments(keysProvided, documentsProvided),
            notes = notes,
            referralSource = referralSource,
            appointmentId = appointmentId,
            calendarColorId = calendarColorId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deliveryPerson = deliveryPersonId?.let {
                DeliveryPerson(
                    id = deliveryPersonId,
                    name = deliveryPersonName!!,
                    phone = deliveryPersonPhoneNumber!!
                )
            }
        )
    }

        fun toDomain(): Visit {
            return Visit(
                id = id?.let { VisitId.of(it) },
                companyId = companyId,
                title = title,
                clientId = ClientId.of(clientId),
                vehicleId = VehicleId.of(vehicleId),
                period = VisitPeriod(startDate, endDate),
                status = status,
                services = services.map { it.toDomain() },
                documents = VisitDocuments(keysProvided, documentsProvided),
                notes = notes,
                referralSource = referralSource,
                appointmentId = appointmentId,
                calendarColorId = calendarColorId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deliveryPerson = deliveryPersonId?.let {
                    DeliveryPerson(
                        id = deliveryPersonId,
                        name = deliveryPersonName!!,
                        phone = deliveryPersonPhoneNumber!!
                    )
                }
            )
    }

    companion object {
        fun fromDomain(visit: Visit): VisitEntity {
            return VisitEntity(
                id = visit.id?.value,
                companyId = visit.companyId,
                title = visit.title,
                clientId = visit.clientId.value,
                vehicleId = visit.vehicleId.value,
                startDate = visit.period.startDate,
                endDate = visit.period.endDate,
                status = visit.status,
                notes = visit.notes,
                referralSource = visit.referralSource,
                appointmentId = visit.appointmentId,
                calendarColorId = visit.calendarColorId,
                keysProvided = visit.documents.keysProvided,
                documentsProvided = visit.documents.documentsProvided,
                createdAt = visit.createdAt,
                updatedAt = visit.updatedAt,
                deliveryPersonId = visit.deliveryPerson?.id,
                deliveryPersonName = visit.deliveryPerson?.name,
                deliveryPersonPhoneNumber = visit.deliveryPerson?.phone,
            )
        }
    }
}