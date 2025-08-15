package com.carslab.crm.production.modules.invoice_templates.infrastructure.repository

import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplateId
import com.carslab.crm.production.modules.invoice_templates.domain.repository.InvoiceTemplateRepository
import com.carslab.crm.production.modules.invoice_templates.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.invoice_templates.infrastructure.mapper.toEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class InvoiceTemplateRepositoryImpl(
    private val jpaRepository: InvoiceTemplateJpaRepository
) : InvoiceTemplateRepository {

    private val logger = LoggerFactory.getLogger(InvoiceTemplateRepositoryImpl::class.java)

    override fun save(template: InvoiceTemplate): InvoiceTemplate {
        logger.debug("Saving template: {} for company: {}", template.id.value, template.companyId)

        val entity = template.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Template saved: {}", savedEntity.id)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: InvoiceTemplateId): InvoiceTemplate? {
        logger.debug("Finding template by ID: {}", id.value)

        val result = jpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)

        if (result == null) {
            logger.debug("Template not found: {}", id.value)
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long): List<InvoiceTemplate> {
        logger.debug("Finding templates for company: {}", companyId)

        val entities = jpaRepository.findByCompanyIdOrderByIsActiveDescCreatedAtDesc(companyId)
        val templates = entities.map { it.toDomain() }

        logger.debug("Found {} templates for company: {}", templates.size, companyId)
        return templates
    }

    @Transactional(readOnly = true)
    override fun findActiveTemplateForCompany(companyId: Long): InvoiceTemplate? {
        logger.debug("Finding active template for company: {}", companyId)

        val result = jpaRepository.findByCompanyIdAndIsActiveTrue(companyId)?.toDomain()

        if (result != null) {
            logger.debug("Found active template: {} for company: {}", result.id.value, companyId)
        } else {
            logger.debug("No active template found for company: {}", companyId)
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun existsByCompanyIdAndName(companyId: Long, name: String): Boolean {
        logger.debug("Checking if template exists: {} for company: {}", name, companyId)

        val exists = jpaRepository.existsByCompanyIdAndName(companyId, name)
        logger.debug("Template exists: {}", exists)

        return exists
    }

    override fun deleteById(id: InvoiceTemplateId): Boolean {
        logger.debug("Deleting template: {}", id.value)

        return try {
            if (jpaRepository.existsById(id.value)) {
                jpaRepository.deleteById(id.value)
                logger.debug("Template deleted: {}", id.value)
                true
            } else {
                logger.debug("Template not found for deletion: {}", id.value)
                false
            }
        } catch (e: Exception) {
            logger.error("Error deleting template: {}", id.value, e)
            false
        }
    }

    override fun deactivateAllForCompany(companyId: Long) {
        logger.debug("Deactivating all templates for company: {}", companyId)

        jpaRepository.deactivateAllForCompany(companyId)
        logger.debug("All templates deactivated for company: {}", companyId)
    }
}