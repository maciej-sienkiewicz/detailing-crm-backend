package com.carslab.crm.production.modules.associations.domain.service

import com.carslab.crm.production.modules.associations.domain.repository.ClientVehicleAssociationRepository
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrimaryOwnerManager(
    private val associationRepository: ClientVehicleAssociationRepository
) {
    private val logger = LoggerFactory.getLogger(PrimaryOwnerManager::class.java)

    fun makePrimaryOwner(clientId: ClientId, vehicleId: VehicleId, companyId: Long) {
        logger.debug("Making client: {} primary owner of vehicle: {} for company: {}",
            clientId.value, vehicleId.value, companyId)

        val currentAssociations = associationRepository.findActiveByVehicleId(vehicleId)

        currentAssociations.forEach { association ->
            if (!association.canBeAccessedBy(companyId)) {
                throw BusinessException("Access denied to association")
            }

            val updated = if (association.clientId == clientId) {
                association.makePrimary()
            } else {
                association.makeSecondary()
            }

            associationRepository.save(updated)
        }

        logger.info("Primary owner updated for vehicle: {} in company: {}", vehicleId.value, companyId)
    }
}