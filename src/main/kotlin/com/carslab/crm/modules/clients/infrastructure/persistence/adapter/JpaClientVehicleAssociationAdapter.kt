package com.carslab.crm.modules.clients.infrastructure.persistence.adapter

import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.ClientVehicleAssociation
import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.modules.clients.domain.port.ClientVehicleAssociationRepositoryDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.ClientVehicleAssociationEntityDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientVehicleAssociationJpaRepositoryDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientJpaRepositoryDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleJpaRepositoryDeprecated
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ClientVehicleAssociationRepositoryDeprecatedAdapter(
    private val associationJpaRepository: ClientVehicleAssociationJpaRepositoryDeprecated,
    private val clientJpaRepositoryDeprecated: ClientJpaRepositoryDeprecated,
    private val vehicleJpaRepositoryDeprecated: VehicleJpaRepositoryDeprecated,
    private val securityContext: SecurityContext
) : ClientVehicleAssociationRepositoryDeprecated {

    override fun save(association: ClientVehicleAssociation): ClientVehicleAssociation {
        val companyId = securityContext.getCurrentCompanyId()

        val clientEntity = clientJpaRepositoryDeprecated.findByIdAndCompanyId(association.clientId.value, companyId)
            .orElseThrow { IllegalStateException("Client not found or access denied") }

        val vehicleEntity = vehicleJpaRepositoryDeprecated.findByIdAndCompanyId(association.vehicleId.value, companyId)
            .orElseThrow { IllegalStateException("Vehicle not found or access denied") }

        val entity = ClientVehicleAssociationEntityDeprecated.fromDomain(
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