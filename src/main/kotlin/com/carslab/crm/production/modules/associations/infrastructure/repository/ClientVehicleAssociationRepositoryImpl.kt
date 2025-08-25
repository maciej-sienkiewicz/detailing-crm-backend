package com.carslab.crm.production.modules.associations.infrastructure.repository

import com.carslab.crm.production.modules.associations.domain.model.AssociationId
import com.carslab.crm.production.modules.associations.domain.model.ClientVehicleAssociation
import com.carslab.crm.production.modules.associations.domain.repository.ClientVehicleAssociationRepository
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
    override fun findById(id: AssociationId): ClientVehicleAssociation? {
        logger.debug("Finding association by ID: {}", id.value)

        return jpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
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
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun deleteById(id: AssociationId): Boolean {
        logger.debug("Deleting association with ID: {}", id.value)

        return try {
            if (jpaRepository.existsById(id.value)) {
                jpaRepository.deleteById(id.value)
                logger.debug("Association deleted: {}", id.value)
                true
            } else {
                logger.debug("Association not found for deletion: {}", id.value)
                false
            }
        } catch (e: Exception) {
            logger.error("Error deleting association: {}", id.value, e)
            false
        }
    }

    override fun endAssociation(clientId: ClientId, vehicleId: VehicleId): Boolean {
        logger.debug("Ending association between client: {} and vehicle: {}", clientId.value, vehicleId.value)

        val updated = jpaRepository.endAssociation(clientId.value, vehicleId.value, LocalDateTime.now())
        return updated > 0
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