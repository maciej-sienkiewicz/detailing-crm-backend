package com.carslab.crm.production.modules.templates.domain.repositories

import com.carslab.crm.production.modules.templates.domain.models.aggregates.Template
import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TemplateRepository {
    fun save(template: Template): Template
    fun findById(templateId: String, companyId: Long): Template?
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Template>
    fun findByCompanyIdAndType(companyId: Long, type: TemplateType, pageable: Pageable): Page<Template>
    fun findByCompanyIdAndIsActive(companyId: Long, isActive: Boolean, pageable: Pageable): Page<Template>
    fun findByCompanyIdAndTypeAndIsActive(
        companyId: Long,
        type: TemplateType,
        isActive: Boolean,
        pageable: Pageable
    ): Page<Template>
    fun findActiveByTypeAndCompany(type: TemplateType, companyId: Long): Template?
    fun findAllActiveByTypeAndCompany(type: TemplateType, companyId: Long): List<Template>
    fun existsById(templateId: String, companyId: Long): Boolean
    fun deleteById(templateId: String, companyId: Long): Boolean
    fun getFileData(templateId: String): ByteArray?
}