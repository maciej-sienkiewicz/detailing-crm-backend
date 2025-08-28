package com.carslab.crm.production.modules.associations.domain.model

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import java.time.LocalDateTime

@JvmInline
value class AssociationId(val value: Long) {
    companion object {
        fun of(value: Long): AssociationId = AssociationId(value)
    }
}

enum class AssociationType {
    OWNER, AUTHORIZED_USER
}

data class ClientVehicleAssociation(
    val clientId: ClientId,
    val vehicleId: VehicleId,
    val companyId: Long,
    val associationType: AssociationType,
    val isPrimary: Boolean,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime?,
    val createdAt: LocalDateTime
) {
    val isActive: Boolean get() = endDate == null

    fun canBeAccessedBy(companyId: Long): Boolean {
        return this.companyId == companyId
    }

    fun end(): ClientVehicleAssociation {
        return copy(endDate = LocalDateTime.now())
    }

    fun makePrimary(): ClientVehicleAssociation {
        return copy(isPrimary = true)
    }

    fun makeSecondary(): ClientVehicleAssociation {
        return copy(isPrimary = false)
    }
}