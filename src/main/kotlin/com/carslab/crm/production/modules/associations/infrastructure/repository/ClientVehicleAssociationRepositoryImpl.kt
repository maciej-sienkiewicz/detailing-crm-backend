package com.carslab.crm.production.modules.associations.infrastructure.repository

import com.carslab.crm.production.modules.associations.domain.model.ClientVehicleAssociation
import com.carslab.crm.production.modules.associations.domain.repository.ClientVehicleAssociationRepository
import com.carslab.crm.production.modules.associations.infrastructure.entity.ClientVehicleAssociationId
import com.carslab.crm.production.modules.associations.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.associations.infrastructure.mapper.toEntity
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class ClientVehicleAssociationRepositoryImpl(
    private val jpaRepository: ClientVehicleAssociationJpaRepository
) : ClientVehicleAssociationRepository {

    private val logger = LoggerFactory.getLogger(ClientVehicleAssociationRepositoryImpl::class.java)

    override fun save(association: ClientVehicleAssociation): ClientVehicleAssociation {
        logger.debug("Saving association between client: {} and vehicle: {}",
            association.clientId.value, association.vehicleId.value)

        val entity = association.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Association saved")
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findActiveByClientId(clientId: ClientId): List<ClientVehicleAssociation> {
        logger.debug("Finding active associations for client: {}", clientId.value)

        return jpaRepository.findByClientIdAndEndDateIsNull(clientId.value)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findActiveByVehicleId(vehicleId: VehicleId): List<ClientVehicleAssociation> {
        logger.debug("Finding active associations for vehicle: {}", vehicleId.value)

        return jpaRepository.findByVehicleIdAndEndDateIsNull(vehicleId.value)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByClientIdAndVehicleId(clientId: ClientId, vehicleId: VehicleId): ClientVehicleAssociation? {
        logger.debug("Finding association for client: {} and vehicle: {}", clientId.value, vehicleId.value)

        return jpaRepository.findByClientIdAndVehicleId(clientId.value, vehicleId.value)
            ?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findActiveByClientIds(clientIds: List<ClientId>): List<ClientVehicleAssociation> {
        if (clientIds.isEmpty()) return emptyList()

        logger.debug("Batch finding active associations for {} clients", clientIds.size)

        val clientIdValues = clientIds.map { it.value }
        return jpaRepository.findByClientIdInAndEndDateIsNull(clientIdValues)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findActiveByVehicleIds(vehicleIds: List<VehicleId>): List<ClientVehicleAssociation> {
        if (vehicleIds.isEmpty()) return emptyList()

        logger.debug("Batch finding active associations for {} vehicles", vehicleIds.size)

        val vehicleIdValues = vehicleIds.map { it.value }
        return jpaRepository.findByVehicleIdInAndEndDateIsNull(vehicleIdValues)
            .map { it.toDomain() }
    }

    override fun endAssociation(clientId: ClientId, vehicleId: VehicleId): Boolean {
        logger.debug("Ending association between client: {} and vehicle: {}", clientId.value, vehicleId.value)

        val updated = jpaRepository.endAssociation(clientId.value, vehicleId.value, LocalDateTime.now())
        return updated > 0
    }

    override fun batchEndAssociations(associations: List<Pair<ClientId, VehicleId>>): Int {
        if (associations.isEmpty()) return 0

        logger.debug("Batch ending {} associations", associations.size)
       
        val associationIds = associations.map { (clientId, vehicleId) ->
            ClientVehicleAssociationId(clientId = clientId.value, vehicleId = vehicleId.value)
        }

        val entitiesToEnd = jpaRepository.findActiveByIds(associationIds)
        
        if (entitiesToEnd.isEmpty()) {
            logger.debug("No active associations found for the given pairs.")
            return 0
        }

        val now = LocalDateTime.now()
        entitiesToEnd.forEach { it.endDate = now }

        jpaRepository.saveAll(entitiesToEnd)

        logger.debug("{} associations ended successfully", entitiesToEnd.size)
        return entitiesToEnd.size
    }

    override fun saveAll(associations: List<ClientVehicleAssociation>): List<ClientVehicleAssociation> {
        if (associations.isEmpty()) return emptyList()

        logger.debug("Batch saving {} associations", associations.size)

        val entities = associations.map { it.toEntity() }
        val savedEntities = jpaRepository.saveAll(entities)

        logger.debug("Batch save completed")
        return savedEntities.map { it.toDomain() }
    }

    override fun deleteByClientId(clientId: ClientId): Int {
        logger.debug("Deleting all associations for client: {}", clientId.value)

        return jpaRepository.deleteByClientId(clientId.value)
    }

    override fun deleteByVehicleId(vehicleId: VehicleId): Int {
        logger.debug("Deleting all associations for vehicle: {}", vehicleId.value)

        return jpaRepository.deleteByVehicleId(vehicleId.value)
    }
}