package com.carslab.crm.production.modules.associations.infrastructure.entity

import com.carslab.crm.production.modules.associations.domain.model.AssociationType
import jakarta.persistence.*
import java.time.LocalDateTime

data class ClientVehicleAssociationId(
    val clientId: Long = 0,
    val vehicleId: Long = 0
)

@Entity
@Table(
    name = "client_vehicle_associations",
    indexes = [
        Index(name = "idx_association_client_id", columnList = "client_id"),
        Index(name = "idx_association_vehicle_id", columnList = "vehicle_id"),
        Index(name = "idx_association_company_id", columnList = "company_id"),
        Index(name = "idx_association_active", columnList = "company_id,end_date"),
        Index(name = "idx_association_client_vehicle", columnList = "client_id,vehicle_id")
    ]
)
@IdClass(ClientVehicleAssociationId::class)
class ClientVehicleAssociationEntity(
    @Column(name = "end_date")
    var endDate: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null,

    @Column(name = "client_id", nullable = false)
    @Id
    val clientId: Long,

    @Column(name = "vehicle_id", nullable = false)
    @Id
    val vehicleId: Long,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "association_type", nullable = false)
    var associationType: AssociationType = AssociationType.OWNER,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDateTime = LocalDateTime.now(),
)