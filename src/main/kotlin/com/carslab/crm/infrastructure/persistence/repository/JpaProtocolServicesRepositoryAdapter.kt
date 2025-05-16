package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.view.protocol.ProtocolServiceView
import com.carslab.crm.domain.port.ProtocolServicesRepository
import com.carslab.crm.infrastructure.persistence.entity.ProtocolServiceEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolServiceJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaProtocolServicesRepositoryAdapter(
    private val protocolServiceJpaRepository: ProtocolServiceJpaRepository,
    private val protocolJpaRepository: ProtocolJpaRepository
) : ProtocolServicesRepository {

    override fun saveServices(services: List<CreateServiceModel>, protocolId: ProtocolId): List<String> {
        // Sprawdź, czy protokół istnieje
        if (!protocolJpaRepository.existsById(protocolId.value)) {
            throw IllegalStateException("Protocol with ID ${protocolId.value} not found")
        }

        val protocolIdLong = protocolId.value.toLong()

        // Usuń istniejące serwisy
        protocolServiceJpaRepository.findByProtocolId(protocolIdLong).forEach {
            protocolServiceJpaRepository.delete(it)
        }

        // Zapisz nowe serwisy
        val savedEntities = services.map { createServiceEntity(it, protocolIdLong) }
            .map { protocolServiceJpaRepository.save(it) }

        return savedEntities.map { it.id.toString() }
    }

    override fun findByProtocolId(protocolId: ProtocolId): List<ProtocolServiceView> {
        return protocolServiceJpaRepository.findByProtocolId(protocolId.value.toLong())
            .map { it.toDomain() }
    }

    private fun createServiceEntity(service: CreateServiceModel, protocolId: Long): ProtocolServiceEntity {
        return ProtocolServiceEntity(
            protocolId = protocolId,
            companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
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