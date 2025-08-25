package com.carslab.crm.production.modules.associations.domain.service

import com.carslab.crm.production.modules.associations.domain.model.ClientVehicleAssociation
import com.carslab.crm.production.modules.associations.domain.repository.ClientVehicleAssociationRepository
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class AssociationAccessValidator(
    private val associationRepository: ClientVehicleAssociationRepository
) {
    fun getAssociationForCompany(clientId: ClientId, vehicleId: VehicleId, companyId: Long): ClientVehicleAssociation {
        val association = associationRepository.findByClientIdAndVehicleId(clientId, vehicleId)
            ?: throw BusinessException("Association not found")

        if (!association.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to association")
        }

        return association
    }
}