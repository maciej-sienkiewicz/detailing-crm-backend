package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.view.protocol.ProtocolServiceView
import com.carslab.crm.domain.port.ProtocolServicesRepository
import com.carslab.crm.infrastructure.persistence.entity.ProtocolEntity
import com.carslab.crm.infrastructure.persistence.entity.ProtocolServiceEntity
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolServiceJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaProtocolServicesRepositoryAdapter(
    private val protocolServiceJpaRepository: ProtocolServiceJpaRepository,
    private val protocolJpaRepository: ProtocolJpaRepository
) : ProtocolServicesRepository {

    override fun saveServices(services: List<CreateServiceModel>, protocolId: ProtocolId): List<String> {
        val protocolEntity = protocolJpaRepository.findById(protocolId.value).orElseThrow {
            IllegalStateException("Protocol with ID ${protocolId.value} not found")
        }

        // Usuń istniejące serwisy
        protocolServiceJpaRepository.findByProtocol_Id(protocolId.value.toLong()).forEach {
            protocolServiceJpaRepository.delete(it)
        }

        // Zapisz nowe serwisy
        val savedEntities = services.map { createServiceEntity(it, protocolEntity) }
            .map { protocolServiceJpaRepository.save(it) }

        return savedEntities.map { it.id.toString() }
    }

    override fun findByProtocolId(protocolId: ProtocolId): List<ProtocolServiceView> {
        return protocolServiceJpaRepository.findByProtocol_Id(protocolId.value.toLong())
            .map { it.toDomain() }
    }

    private fun createServiceEntity(service: CreateServiceModel, protocolEntity: ProtocolEntity): ProtocolServiceEntity {
        return ProtocolServiceEntity(
            protocol = protocolEntity,
            name = service.name,
            basePrice = service.basePrice.amount.toBigDecimal(),
            finalPrice = service.finalPrice.amount.toBigDecimal(),
            quantity = service.quantity.toInt(),
            approvalStatus = service.approvalStatus,
            note = service.note,
            discountType = service.discount?.type,
            discountValue = service.discount?.value?.toBigDecimal()
        )
    }
}