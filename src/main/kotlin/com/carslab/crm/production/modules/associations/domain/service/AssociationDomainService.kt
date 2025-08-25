package com.carslab.crm.production.modules.associations.domain.service

import com.carslab.crm.production.modules.associations.domain.model.AssociationType
import com.carslab.crm.production.modules.associations.domain.model.ClientVehicleAssociation
import com.carslab.crm.production.modules.associations.domain.repository.ClientVehicleAssociationRepository
import com.carslab.crm.production.modules.clients.application.service.ClientStatisticsCommandService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AssociationDomainService(
    private val associationRepository: ClientVehicleAssociationRepository,
    private val clientStatisticsCommandService: ClientStatisticsCommandService,
    private val associationCreator: AssociationCreator,
    private val associationAccessValidator: AssociationAccessValidator,
    private val primaryOwnerManager: PrimaryOwnerManager
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
            logger.debug("Active association already exists between client: {} and vehicle: {}",
                clientId.value, vehicleId.value)
            return existingAssociation
        }

        val association = associationCreator.create(clientId, vehicleId, companyId, associationType, isPrimary)
        val saved = associationRepository.save(association)

        clientStatisticsCommandService.incrementVehicleCount(clientId.value.toString())

        logger.info("Association created for company: {}", companyId)
        return saved
    }

    fun createAssociations(
        associations: List<Triple<ClientId, VehicleId, Boolean>>,
        companyId: Long,
        associationType: AssociationType = AssociationType.OWNER
    ): List<ClientVehicleAssociation> {
        if (associations.isEmpty()) return emptyList()

        logger.debug("Batch creating {} associations for company: {}", associations.size, companyId)

        val savedAssociations = associations.map { (clientId, vehicleId, isPrimary) ->
            val association = associationCreator.create(clientId, vehicleId, companyId, associationType, isPrimary)
            associationRepository.save(association)
        }

        associations.forEach { (clientId, _, _) ->
            clientStatisticsCommandService.incrementVehicleCount(clientId.value.toString())
        }

        logger.info("Batch created {} associations for company: {}", savedAssociations.size, companyId)
        return savedAssociations
    }

    fun endAssociation(clientId: ClientId, vehicleId: VehicleId, companyId: Long): Boolean {
        logger.debug("Ending association between client: {} and vehicle: {} for company: {}",
            clientId.value, vehicleId.value, companyId)

        val association = associationAccessValidator.getAssociationForCompany(clientId, vehicleId, companyId)

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

    fun endAssociations(
        associations: List<Pair<ClientId, VehicleId>>,
        companyId: Long
    ): Int {
        if (associations.isEmpty()) return 0

        logger.debug("Batch ending {} associations for company: {}", associations.size, companyId)

        associations.forEach { (clientId, vehicleId) ->
            associationAccessValidator.getAssociationForCompany(clientId, vehicleId, companyId)
        }

        val ended = associationRepository.batchEndAssociations(associations)

        if (ended > 0) {
            logger.info("Batch ended {} associations for company: {}", ended, companyId)
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
        logger.debug("Batch getting owners for {} vehicles", vehicleIds.size)

        if (vehicleIds.isEmpty()) {
            return emptyMap()
        }

        val allAssociations = associationRepository.findActiveByVehicleIds(vehicleIds)

        val result = allAssociations.groupBy { it.vehicleId }
            .mapValues { (_, associations) -> associations.map { it.clientId } }

        logger.debug("Resolved owners for {} vehicles using single batch query", result.size)
        return result
    }

    fun getClientVehiclesMap(clientIds: List<ClientId>): Map<ClientId, List<VehicleId>> {
        logger.debug("Batch getting vehicles for {} clients", clientIds.size)

        if (clientIds.isEmpty()) {
            return emptyMap()
        }

        val allAssociations = associationRepository.findActiveByClientIds(clientIds)

        val result = allAssociations.groupBy { it.clientId }
            .mapValues { (_, associations) -> associations.map { it.vehicleId } }

        logger.debug("Resolved vehicles for {} clients using single batch query", result.size)
        return result
    }

    fun makePrimaryOwner(clientId: ClientId, vehicleId: VehicleId, companyId: Long) {
        primaryOwnerManager.makePrimaryOwner(clientId, vehicleId, companyId)
    }
}