package com.carslab.crm.clients.infrastructure.persistence.adapter

import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.clients.domain.model.ClientVehicleAssociation
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.clients.domain.port.ClientVehicleAssociationRepository
import com.carslab.crm.clients.infrastructure.persistence.entity.ClientVehicleAssociationEntity
import com.carslab.crm.clients.infrastructure.persistence.repository.ClientVehicleAssociationJpaRepository
import com.carslab.crm.clients.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.clients.infrastructure.persistence.repository.VehicleJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ClientVehicleAssociationRepositoryAdapter(
    private val associationJpaRepository: ClientVehicleAssociationJpaRepository,
    private val clientJpaRepository: ClientJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val securityContext: SecurityContext
) : ClientVehicleAssociationRepository {

    override fun save(association: ClientVehicleAssociation): ClientVehicleAssociation {
        val companyId = securityContext.getCurrentCompanyId()

        val clientEntity = clientJpaRepository.findByIdAndCompanyId(association.clientId.value, companyId)
            .orElseThrow { IllegalStateException("Client not found or access denied") }

        val vehicleEntity = vehicleJpaRepository.findByIdAndCompanyId(association.vehicleId.value, companyId)
            .orElseThrow { IllegalStateException("Vehicle not found or access denied") }

        val entity = ClientVehicleAssociationEntity.fromDomain(
            association = association,
            client = clientEntity,
            vehicle = vehicleEntity,
            companyId = companyId
        )

        val savedEntity = associationJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findByClientId(clientId: ClientId): List<ClientVehicleAssociation> {
        val companyId = securityContext.getCurrentCompanyId()
        return associationJpaRepository.findByClientIdAndCompanyId(clientId.value, companyId)
            .map { it.toDomain() }
    }

    override fun findByVehicleId(vehicleId: VehicleId): List<ClientVehicleAssociation> {
        val companyId = securityContext.getCurrentCompanyId()
        return associationJpaRepository.findByVehicleIdAndCompanyId(vehicleId.value, companyId)
            .map { it.toDomain() }
    }

    override fun findActiveByClientId(clientId: ClientId): List<ClientVehicleAssociation> {
        val companyId = securityContext.getCurrentCompanyId()
        return associationJpaRepository.findActiveByClientIdAndCompanyId(clientId.value, companyId)
            .map { it.toDomain() }
    }

    override fun findActiveByVehicleId(vehicleId: VehicleId): List<ClientVehicleAssociation> {
        val companyId = securityContext.getCurrentCompanyId()
        return associationJpaRepository.findActiveByVehicleIdAndCompanyId(vehicleId.value, companyId)
            .map { it.toDomain() }
    }

    override fun findByClientIdAndVehicleId(clientId: ClientId, vehicleId: VehicleId): ClientVehicleAssociation? {
        val companyId = securityContext.getCurrentCompanyId()
        return associationJpaRepository.findByClientIdAndVehicleIdAndCompanyId(
            clientId.value, vehicleId.value, companyId
        ).map { it.toDomain() }.orElse(null)
    }

    override fun deleteByClientIdAndVehicleId(clientId: ClientId, vehicleId: VehicleId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        val deleted = associationJpaRepository.endAssociation(
            clientId.value, vehicleId.value, LocalDateTime.now(), companyId
        )
        return deleted > 0
    }

    override fun deleteByClientId(clientId: ClientId): Int {
        val companyId = securityContext.getCurrentCompanyId()
        return associationJpaRepository.deleteByClientIdAndCompanyId(clientId.value, companyId)
    }

    override fun deleteByVehicleId(vehicleId: VehicleId): Int {
        val companyId = securityContext.getCurrentCompanyId()
        return associationJpaRepository.deleteByVehicleIdAndCompanyId(vehicleId.value, companyId)
    }
}