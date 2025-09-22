package com.carslab.crm.production.modules.services.infrastructure.repository

import com.carslab.crm.production.modules.services.domain.model.Service
import com.carslab.crm.production.modules.services.domain.model.ServiceId
import com.carslab.crm.production.modules.services.domain.repository.ServiceRepository
import com.carslab.crm.production.modules.services.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.services.infrastructure.mapper.toEntity
import com.carslab.crm.production.shared.observability.annotations.DatabaseMonitored
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class ServiceRepositoryImpl(
    private val jpaRepository: ServiceJpaRepository
) : ServiceRepository {

    private val logger = LoggerFactory.getLogger(ServiceRepositoryImpl::class.java)

    @DatabaseMonitored(repository = "service", method = "save", operation = "insert")
    override fun save(service: Service): Service {
        logger.debug("Saving service: {} for company: {}", service.id.value, service.companyId)

        val entity = service.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Service saved: {}", savedEntity.id)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "service", method = "findById", operation = "select")
    override fun findById(id: ServiceId): Service? {
        logger.debug("Finding service by ID: {}", id.value)

        val result = jpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)

        if (result == null) {
            logger.debug("Service not found: {}", id.value)
        }

        return result
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "service", method = "findActiveById", operation = "select")
    override fun findActiveById(id: ServiceId): Service? {
        logger.debug("Finding active service by ID: {}", id.value)

        val result = jpaRepository.findByIdAndIsActiveTrue(id.value)?.toDomain()

        if (result == null) {
            logger.debug("Active service not found: {}", id.value)
        }

        return result
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "service", method = "findActiveByCompanyId", operation = "select")
    override fun findActiveByCompanyId(companyId: Long): List<Service> {
        logger.debug("Finding active services for company: {}", companyId)

        val entities = jpaRepository.findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(companyId)
        val services = entities.map { it.toDomain() }

        logger.debug("Found {} active services for company: {}", services.size, companyId)
        return services
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "service", method = "existsByCompanyIdAndName", operation = "select")
    override fun existsByCompanyIdAndName(companyId: Long, name: String): Boolean {
        logger.debug("Checking if active service exists: {} for company: {}", name, companyId)

        val exists = jpaRepository.existsByCompanyIdAndNameAndIsActiveTrue(companyId, name)
        logger.debug("Active service exists: {}", exists)

        return exists
    }

    @DatabaseMonitored(repository = "service", method = "deleteById", operation = "delete")
    override fun deleteById(id: ServiceId): Boolean {
        logger.debug("Deleting service: {}", id.value)

        return try {
            if (jpaRepository.existsById(id.value)) {
                jpaRepository.deleteById(id.value)
                logger.debug("Service deleted: {}", id.value)
                true
            } else {
                logger.debug("Service not found for deletion: {}", id.value)
                false
            }
        } catch (e: Exception) {
            logger.error("Error deleting service: {}", id.value, e)
            false
        }
    }

    @DatabaseMonitored(repository = "service", method = "deactivateById", operation = "update")
    override fun deactivateById(id: ServiceId) {
        logger.debug("Deactivating service: {}", id.value)

        jpaRepository.deactivateById(id.value)
        logger.debug("Service deactivated: {}", id.value)
    }
}