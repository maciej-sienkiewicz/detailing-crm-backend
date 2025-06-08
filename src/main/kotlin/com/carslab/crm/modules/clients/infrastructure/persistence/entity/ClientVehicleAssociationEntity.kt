package com.carslab.crm.modules.clients.infrastructure.persistence.entity

import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.ClientVehicleAssociation
import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.modules.clients.domain.model.VehicleRelationshipType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "client_vehicle_associations",
    indexes = [
        Index(name = "idx_association_client_id", columnList = "client_id"),
        Index(name = "idx_association_vehicle_id", columnList = "vehicle_id"),
        Index(name = "idx_association_active", columnList = "end_date"),
        Index(name = "idx_association_company_id", columnList = "company_id")
    ]
)
class ClientVehicleAssociationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    val client: ClientEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    val vehicle: VehicleEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    var relationshipType: VehicleRelationshipType = VehicleRelationshipType.OWNER,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "end_date")
    var endDate: LocalDateTime? = null,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): ClientVehicleAssociation = ClientVehicleAssociation(
        clientId = ClientId.of(client.id!!),
        vehicleId = VehicleId.of(vehicle.id!!),
        relationshipType = relationshipType,
        startDate = startDate,
        endDate = endDate,
        isPrimary = isPrimary
    )

    companion object {
        fun fromDomain(
            association: ClientVehicleAssociation,
            client: ClientEntity,
            vehicle: VehicleEntity,
            companyId: Long
        ): ClientVehicleAssociationEntity = ClientVehicleAssociationEntity(
            companyId = companyId,
            client = client,
            vehicle = vehicle,
            relationshipType = association.relationshipType,
            startDate = association.startDate,
            endDate = association.endDate,
            isPrimary = association.isPrimary
        )
    }
}