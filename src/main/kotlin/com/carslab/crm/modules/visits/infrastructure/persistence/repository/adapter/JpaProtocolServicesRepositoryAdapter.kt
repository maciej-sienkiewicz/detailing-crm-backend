package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.view.protocol.ProtocolServiceView
import com.carslab.crm.modules.visits.domain.ports.ProtocolServicesRepository
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolServiceJpaRepository
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolServiceEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository

@Repository
class JpaProtocolServicesRepositoryAdapter(
    private val protocolServiceJpaRepository: ProtocolServiceJpaRepository,
    private val protocolJpaRepository: ProtocolJpaRepository
) : ProtocolServicesRepository {

    override fun saveServices(services: List<CreateServiceModel>, protocolId: ProtocolId): List<String> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // Sprawdź, czy protokół istnieje i należy do tej samej firmy
        protocolJpaRepository.findByCompanyIdAndId(companyId, protocolId.value.toLong())
            .orElse(null) ?: throw IllegalStateException("Protocol with ID ${protocolId.value} not found or access denied")

        val protocolIdLong = protocolId.value.toLong()

        // Usuń istniejące serwisy
        protocolServiceJpaRepository.findByProtocolIdAndCompanyId(protocolIdLong, companyId).forEach {
            protocolServiceJpaRepository.delete(it)
        }

        // Zapisz nowe serwisy
        val savedEntities = services.map { createServiceEntity(it, protocolIdLong, companyId) }
            .map { protocolServiceJpaRepository.save(it) }

        return savedEntities.map { it.id.toString() }
    }

    override fun findByProtocolId(protocolId: ProtocolId): List<ProtocolServiceView> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return protocolServiceJpaRepository.findByProtocolIdAndCompanyId(protocolId.value.toLong(), companyId)
            .map { it.toDomain() }
    }

    private fun createServiceEntity(service: CreateServiceModel, protocolId: Long, companyId: Long): ProtocolServiceEntity {
        return ProtocolServiceEntity(
            protocolId = protocolId,
            companyId = companyId,
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