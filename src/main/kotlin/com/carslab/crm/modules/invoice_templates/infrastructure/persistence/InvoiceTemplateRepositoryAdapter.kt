package com.carslab.crm.modules.invoice_templates.infrastructure.persistence

import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplateId
import com.carslab.crm.modules.invoice_templates.domain.model.TemplateType
import com.carslab.crm.modules.invoice_templates.domain.ports.InvoiceTemplateRepository
import com.carslab.crm.modules.invoice_templates.infrastructure.persistence.entity.InvoiceTemplateEntity
import com.carslab.crm.modules.invoice_templates.infrastructure.persistence.repository.InvoiceTemplateJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class InvoiceTemplateRepositoryAdapter(
    private val jpaRepository: InvoiceTemplateJpaRepository
) : InvoiceTemplateRepository {

    @Transactional
    override fun save(template: InvoiceTemplate): InvoiceTemplate {
        val entity = InvoiceTemplateEntity.fromDomain(template)
        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: InvoiceTemplateId): InvoiceTemplate? {
        return jpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long): List<InvoiceTemplate> {
        return jpaRepository.findByCompanyIdOrderByIsActiveDescCreatedAtDesc(companyId)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findActiveTemplateForCompany(companyId: Long): InvoiceTemplate? {
        return jpaRepository.findByCompanyIdAndIsActiveTrue(companyId)
            .firstOrNull()?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findSystemDefaultTemplate(): InvoiceTemplate? {
        return jpaRepository.findByTemplateTypeAndIsActiveTrue(TemplateType.SYSTEM_DEFAULT)
            .firstOrNull()?.toDomain()
    }

    @Transactional
    override fun deactivateAllTemplatesForCompany(companyId: Long) {
        jpaRepository.deactivateAllForCompany(companyId)
    }

    @Transactional(readOnly = true)
    override fun findByType(type: TemplateType): List<InvoiceTemplate> {
        return jpaRepository.findByTemplateType(type)
            .map { it.toDomain() }
    }

    @Transactional
    override fun deleteById(id: InvoiceTemplateId): Boolean {
        return try {
            if (jpaRepository.existsById(id.value)) {
                jpaRepository.deleteById(id.value)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}