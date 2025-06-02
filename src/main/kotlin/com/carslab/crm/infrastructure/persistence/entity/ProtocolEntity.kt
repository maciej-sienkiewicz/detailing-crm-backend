package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.ReferralSource
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import com.carslab.crm.domain.model.view.protocol.ProtocolView
import jakarta.persistence.*
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime

@Entity
@Table(name = "protocols")
class ProtocolEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(nullable = false)
    var companyId: Long,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var calendarColorId: String,

    // Zmienione z obiektu na ID
    @Column(name = "vehicle_id", nullable = false)
    var vehicleId: Long,

    // Zmienione z obiektu na ID
    @Column(name = "client_id", nullable = false)
    var clientId: Long,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDateTime,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProtocolStatus,

    @Column(nullable = true)
    var notes: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "referral_source", nullable = true)
    var referralSource: ReferralSource? = null,

    @Column(name = "other_source_details", nullable = true)
    var otherSourceDetails: String? = null,

    @Column(name = "keys_provided")
    var keysProvided: Boolean = false,

    @Column(name = "documents_provided")
    var documentsProvided: Boolean = false,

    @Column(name = "appointment_id", nullable = true)
    var appointmentId: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "status_updated_at", nullable = false)
    var statusUpdatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "protocolId", cascade = [CascadeType.ALL], orphanRemoval = true)
    var services: MutableList<ProtocolServiceEntity> = mutableListOf(),

    @OneToMany(mappedBy = "protocolId", cascade = [CascadeType.ALL], orphanRemoval = true)
    var comments: MutableList<ProtocolCommentEntity> = mutableListOf(),

    @OneToMany(mappedBy = "protocolId", cascade = [CascadeType.ALL], orphanRemoval = true)
    var images: MutableList<VehicleImageEntity> = mutableListOf()
) {
    fun toDomainView(): ProtocolView = ProtocolView(
        id = ProtocolId(id.toString()),
        title = title,
        vehicleId = VehicleId(vehicleId),
        clientId = ClientId(clientId),
        period = com.carslab.crm.domain.model.ServicePeriod(
            startDate = startDate,
            endDate = endDate
        ),
        status = status,
        notes = notes,
        createdAt = createdAt,
        keysProvided = keysProvided,
        documentsProvided = documentsProvided,
        calendarColorId = CalendarColorId(calendarColorId),
    )

    companion object {
        fun fromDomainView(domain: ProtocolView): ProtocolEntity {
            return ProtocolEntity(
                title = domain.title,
                companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                vehicleId = domain.vehicleId.value,
                clientId = domain.clientId.value,
                startDate = domain.period.startDate,
                endDate = domain.period.endDate,
                status = domain.status,
                notes = domain.notes,
                createdAt = domain.createdAt,
                updatedAt = LocalDateTime.now(),
                statusUpdatedAt = LocalDateTime.now(),
                calendarColorId = domain.calendarColorId.value,
            )
        }
    }
}