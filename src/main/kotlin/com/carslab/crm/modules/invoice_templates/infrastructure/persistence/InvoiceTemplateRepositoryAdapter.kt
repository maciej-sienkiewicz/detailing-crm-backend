package com.carslab.crm.modules.invoice_templates.infrastructure.persistence

import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplateId
import com.carslab.crm.modules.invoice_templates.domain.model.TemplateType
import com.carslab.crm.modules.invoice_templates.domain.ports.InvoiceTemplateRepository
import com.carslab.crm.modules.invoice_templates.infrastructure.persistence.entity.InvoiceTemplateEntity
import com.carslab.crm.modules.invoice_templates.infrastructure.persistence.repository.InvoiceTemplateJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class InvoiceTemplateRepositoryAdapter(
    private val jpaRepository: InvoiceTemplateJpaRepository
) : InvoiceTemplateRepository {

    private val logger = LoggerFactory.getLogger(InvoiceTemplateRepositoryAdapter::class.java)

    @Transactional
    override fun save(template: InvoiceTemplate): InvoiceTemplate {
        logger.debug("Saving template {} for company {}", template.id.value, template.companyId)

        val entity = InvoiceTemplateEntity.fromDomain(template)
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Template {} saved successfully", savedEntity.id)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: InvoiceTemplateId): InvoiceTemplate? {
        logger.debug("Finding template by ID: {}", id.value)

        val result = jpaRepository.findById(id.value)
            .map {
                logger.debug("Found template: {} for company: {}", it.id, it.companyId)
                it.toDomain()
            }
            .orElse(null)

        if (result == null) {
            logger.debug("Template not found: {}", id.value)
        }

        return result
    }

    @Transactional(readOnly = false)
    override fun findByCompanyId(companyId: Long): List<InvoiceTemplate> {
        logger.debug("Finding templates for company: {}", companyId)
        jpaRepository.deleteAll()
        val entities = jpaRepository.findByCompanyIdOrderByIsActiveDescCreatedAtDesc(companyId)
        val templates = entities.map { it.toDomain() }

        logger.debug("Found {} templates for company: {}", templates.size, companyId)
        return templates
    }

    @Transactional(readOnly = true)
    override fun findActiveTemplateForCompany(companyId: Long): InvoiceTemplate? {
        logger.debug("Finding active template for company: {}", companyId)

        val result = jpaRepository.findByCompanyIdAndIsActiveTrue(companyId)
            .firstOrNull()?.toDomain()

        if (result != null) {
            logger.debug("Found active template {} for company: {}", result.id.value, companyId)
        } else {
            logger.debug("No active template found for company: {}", companyId)
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findSystemDefaultTemplate(): InvoiceTemplate? {
        logger.debug("Finding system default template")

        val result = jpaRepository.findByTemplateTypeAndIsActiveTrue(TemplateType.SYSTEM_DEFAULT)
            .firstOrNull()?.toDomain()

        if (result != null) {
            logger.debug("Found system default template: {}", result.id.value)
        } else {
            logger.debug("No system default template found")
        }

        return result
    }

    @Transactional
    override fun deactivateAllTemplatesForCompany(companyId: Long) {
        logger.debug("Deactivating all templates for company: {}", companyId)

        val count = jpaRepository.countActiveTemplatesForCompany(companyId)
        jpaRepository.deactivateAllForCompany(companyId)

        logger.debug("Deactivated {} templates for company: {}", count, companyId)
    }

    @Transactional(readOnly = true)
    override fun findByType(type: TemplateType): List<InvoiceTemplate> {
        logger.debug("Finding templates by type: {}", type)

        val templates = jpaRepository.findByTemplateType(type)
            .map { it.toDomain() }

        logger.debug("Found {} templates of type: {}", templates.size, type)
        return templates
    }

    @Transactional
    override fun deleteById(id: InvoiceTemplateId): Boolean {
        logger.debug("Deleting template: {}", id.value)

        return try {
            if (jpaRepository.existsById(id.value)) {
                jpaRepository.deleteById(id.value)
                logger.debug("Template {} deleted successfully", id.value)
                true
            } else {
                logger.debug("Template {} not found for deletion", id.value)
                false
            }
        } catch (e: Exception) {
            logger.error("Error deleting template {}", id.value, e)
            false
        }
    }
}