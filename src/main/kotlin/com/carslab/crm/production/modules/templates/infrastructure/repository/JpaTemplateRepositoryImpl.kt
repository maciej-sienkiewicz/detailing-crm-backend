package com.carslab.crm.production.modules.templates.infrastructure.repository

import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.production.modules.templates.domain.models.aggregates.Template
import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import com.carslab.crm.production.modules.templates.domain.repositories.TemplateRepository
import com.carslab.crm.production.modules.templates.infrastructure.entity.TemplateEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaTemplateRepositoryImpl(
    private val templateJpaRepository: TemplateJpaRepository,
    private val storageService: UniversalStorageService
) : TemplateRepository {

    override fun save(template: Template): Template {
        val entity = TemplateEntity.fromDomain(template)
        val savedEntity = templateJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(templateId: String, companyId: Long): Template? {
        return templateJpaRepository.findByIdAndCompanyId(templateId, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Template> {
        return templateJpaRepository.findByCompanyId(companyId, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByCompanyIdAndType(companyId: Long, type: TemplateType, pageable: Pageable): Page<Template> {
        return templateJpaRepository.findByCompanyIdAndType(companyId, type, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByCompanyIdAndIsActive(companyId: Long, isActive: Boolean, pageable: Pageable): Page<Template> {
        return templateJpaRepository.findByCompanyIdAndIsActive(companyId, isActive, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByCompanyIdAndTypeAndIsActive(
        companyId: Long,
        type: TemplateType,
        isActive: Boolean,
        pageable: Pageable
    ): Page<Template> {
        return templateJpaRepository.findByCompanyIdAndTypeAndIsActive(companyId, type, isActive, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findActiveByTypeAndCompany(type: TemplateType, companyId: Long): Template? {
        return templateJpaRepository.findActiveByTypeAndCompany(companyId, type)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findAllActiveByTypeAndCompany(type: TemplateType, companyId: Long): List<Template> {
        return templateJpaRepository.findAllActiveByTypeAndCompany(companyId, type)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun existsById(templateId: String, companyId: Long): Boolean {
        return templateJpaRepository.existsByIdAndCompanyId(templateId, companyId)
    }

    override fun deleteById(templateId: String, companyId: Long): Boolean {
        return templateJpaRepository.deleteByIdAndCompanyId(templateId, companyId) > 0
    }

    @Transactional(readOnly = true)
    override fun getFileData(templateId: String): ByteArray? {
        return try {
            storageService.retrieveFile(templateId)
        } catch (e: Exception) {
            null
        }
    }
}