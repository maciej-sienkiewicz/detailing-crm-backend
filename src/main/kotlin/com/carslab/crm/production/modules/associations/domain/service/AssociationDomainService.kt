package com.carslab.crm.production.modules.associations.domain.service

import com.carslab.crm.production.modules.associations.domain.model.AssociationType
import com.carslab.crm.production.modules.associations.domain.model.ClientVehicleAssociation
import com.carslab.crm.production.modules.associations.domain.repository.ClientVehicleAssociationRepository
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AssociationDomainService(
    private val associationRepository: ClientVehicleAssociationRepository
) {
    private val logger = LoggerFactory.getLogger(AssociationDomainService::class.java)

    fun createAssociation(
        clientId: ClientId,
        vehicleId: VehicleId,
        companyId: Long,
        associationType: AssociationType = AssociationType.OWNER,
        isPrimary: Boolean = false
    ): ClientVehicleAssociation {
        logger.debug("Creating association between client: {} and vehicle: {} for company: {}",
            clientId.value, vehicleId.value, companyId)

        val existingAssociation = associationRepository.findByClientIdAndVehicleId(clientId, vehicleId)
        if (existingAssociation?.isActive == true) {
            throw BusinessException("Active association already exists between client and vehicle")
        }

        val association = ClientVehicleAssociation(
            id = null,
            clientId = clientId,
            vehicleId = vehicleId,
            companyId = companyId,
            associationType = associationType,
            isPrimary = isPrimary,
            startDate = LocalDateTime.now(),
            endDate = null,
            createdAt = LocalDateTime.now()
        )

        val saved = associationRepository.save(association)
        logger.info("Association created: {} for company: {}", saved.id?.value, companyId)
        return saved
    }

    fun endAssociation(clientId: ClientId, vehicleId: VehicleId, companyId: Long): Boolean {
        logger.debug("Ending association between client: {} and vehicle: {} for company: {}",
            clientId.value, vehicleId.value, companyId)

        val association = associationRepository.findByClientIdAndVehicleId(clientId, vehicleId)
            ?: throw BusinessException("Association not found")

        if (!association.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to association")
        }

        if (!association.isActive) {
            throw BusinessException("Association is already ended")
        }

        val ended = associationRepository.endAssociation(clientId, vehicleId)
        if (ended) {
            logger.info("Association ended between client: {} and vehicle: {} for company: {}",
                clientId.value, vehicleId.value, companyId)
        }

        return ended
    }

    fun getClientVehicles(clientId: ClientId): List<VehicleId> {
        return associationRepository.findActiveByClientId(clientId)
            .map { it.vehicleId }
    }

    fun getVehicleClients(vehicleId: VehicleId): List<ClientId> {
        return associationRepository.findActiveByVehicleId(vehicleId)
            .map { it.clientId }
    }

    fun getVehicleOwnersMap(vehicleIds: List<VehicleId>): Map<VehicleId, List<ClientId>> {
        logger.debug("Getting owners for {} vehicles", vehicleIds.size)

        if (vehicleIds.isEmpty()) {
            return emptyMap()
        }

        val allAssociations = vehicleIds.flatMap { vehicleId ->
            associationRepository.findActiveByVehicleId(vehicleId)
        }

        return allAssociations.groupBy { it.vehicleId }
            .mapValues { (_, associations) -> associations.map { it.clientId } }
    }

    fun getClientVehiclesMap(clientIds: List<ClientId>): Map<ClientId, List<VehicleId>> {
        logger.debug("Getting vehicles for {} clients", clientIds.size)

        if (clientIds.isEmpty()) {
            return emptyMap()
        }

        val allAssociations = clientIds.flatMap { clientId ->
            associationRepository.findActiveByClientId(clientId)
        }

        return allAssociations.groupBy { it.clientId }
            .mapValues { (_, associations) -> associations.map { it.vehicleId } }
    }

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