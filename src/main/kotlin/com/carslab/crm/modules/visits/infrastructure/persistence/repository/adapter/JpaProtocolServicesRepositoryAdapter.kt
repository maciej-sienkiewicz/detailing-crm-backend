package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.view.protocol.ProtocolServiceView
import com.carslab.crm.modules.visits.domain.ports.ProtocolServicesRepository
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolServiceJpaRepository
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolServiceEntityDeprecated
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaProtocolServicesRepositoryAdapter(
    private val protocolServiceJpaRepository: ProtocolServiceJpaRepository,
    private val protocolJpaRepository: ProtocolJpaRepository
) : ProtocolServicesRepository {

    private val logger = LoggerFactory.getLogger(JpaProtocolServicesRepositoryAdapter::class.java)
    
    @Transactional
    override fun saveServices(services: List<CreateServiceModel>, protocolId: ProtocolId): List<String> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // Sprawdź, czy protokół istnieje i należy do tej samej firmy
        protocolJpaRepository.findByCompanyIdAndId(companyId, protocolId.value.toLong())
            .orElse(null) ?: throw IllegalStateException("Protocol with ID ${protocolId.value} not found or access denied")

        val protocolIdLong = protocolId.value.toLong()

        try {
            // 1. USUŃ wszystkie istniejące usługi protokołu
            val existingServices = protocolServiceJpaRepository.findByProtocolIdAndCompanyId(protocolIdLong, companyId)
            if (existingServices.isNotEmpty()) {
                protocolServiceJpaRepository.deleteAll(existingServices)
                logger.debug("Deleted ${existingServices.size} existing services for protocol: ${protocolId.value}")
            }

            // 2. Jeśli lista jest pusta, kończymy (wszystkie usługi zostały usunięte)
            if (services.isEmpty()) {
                logger.info("All services removed for protocol: ${protocolId.value}")
                return emptyList()
            }

            // 3. DODAJ nowe usługi
            val newServiceEntities = services.map { service ->
                createServiceEntity(service, protocolIdLong, companyId)
            }

            val savedEntities = protocolServiceJpaRepository.saveAll(newServiceEntities)
            val savedIds = savedEntities.map { it.id.toString() }

            logger.info("Successfully replaced services for protocol ${protocolId.value}: added ${savedIds.size} new services")

            return savedIds

        } catch (e: Exception) {
            logger.error("Error updating services for protocol ${protocolId.value}", e)
            throw RuntimeException("Failed to update protocol services", e)
        }
    }

    @Transactional(readOnly = true)
    override fun findByProtocolId(protocolId: ProtocolId): List<ProtocolServiceView> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        return try {
            protocolServiceJpaRepository.findByProtocolIdAndCompanyId(protocolId.value.toLong(), companyId)
                .map { it.toDomain() }
        } catch (e: Exception) {
            logger.error("Error finding services for protocol ${protocolId.value}", e)
            throw RuntimeException("Failed to find protocol services", e)
        }
    }

    /**
     * Usuwa wszystkie usługi dla protokołu (pomocnicza metoda)
     */
    @Transactional
    fun deleteAllServicesForProtocol(protocolId: ProtocolId): Int {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val protocolIdLong = protocolId.value.toLong()

        return try {
            val existingServices = protocolServiceJpaRepository.findByProtocolIdAndCompanyId(protocolIdLong, companyId)
            if (existingServices.isNotEmpty()) {
                protocolServiceJpaRepository.deleteAll(existingServices)
                logger.info("Deleted ${existingServices.size} services for protocol: ${protocolId.value}")
            }
            existingServices.size
        } catch (e: Exception) {
            logger.error("Error deleting services for protocol ${protocolId.value}", e)
            throw RuntimeException("Failed to delete protocol services", e)
        }
    }

    /**
     * Dodaje pojedynczą usługę do protokołu (pomocnicza metoda)
     */
    @Transactional
    fun addSingleService(service: CreateServiceModel, protocolId: ProtocolId): String {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val protocolIdLong = protocolId.value.toLong()

        // Sprawdź, czy protokół istnieje
        protocolJpaRepository.findByCompanyIdAndId(companyId, protocolIdLong)
            .orElse(null) ?: throw IllegalStateException("Protocol with ID ${protocolId.value} not found or access denied")

        return try {
            val serviceEntity = createServiceEntity(service, protocolIdLong, companyId)
            val savedEntity = protocolServiceJpaRepository.save(serviceEntity)

            logger.debug("Added single service to protocol ${protocolId.value}: ${service.name}")
            savedEntity.id.toString()
        } catch (e: Exception) {
            logger.error("Error adding service to protocol ${protocolId.value}", e)
            throw RuntimeException("Failed to add service to protocol", e)
        }
    }

    /**
     * Tworzy encję usługi z modelu domenowego
     */
    private fun createServiceEntity(service: CreateServiceModel, protocolId: Long, companyId: Long): ProtocolServiceEntityDeprecated {
        return ProtocolServiceEntityDeprecated(
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