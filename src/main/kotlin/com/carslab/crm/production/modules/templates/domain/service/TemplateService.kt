package com.carslab.crm.production.modules.templates.domain.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.production.modules.templates.domain.command.CreateTemplateCommand
import com.carslab.crm.production.modules.templates.domain.command.UpdateTemplateCommand
import com.carslab.crm.production.modules.templates.domain.models.aggregates.Template
import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import com.carslab.crm.production.modules.templates.domain.repositories.TemplateRepository
import com.carslab.crm.production.modules.templates.domain.validator.TemplateValidator
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class TemplateService(
    private val templateRepository: TemplateRepository,
    private val storageService: UniversalStorageService,
    private val templateValidator: TemplateValidator,
) {

    fun createTemplate(command: CreateTemplateCommand): Template {
        templateValidator.validateFile(command.file)
        templateValidator.validateTemplateData(command.name, command.type)

        if (command.isActive) {
            deactivateExistingActiveTemplates(command.companyId, command.type)
        }

        val storageId = storageService.storeFile(
            UniversalStoreRequest(
                file = command.file,
                originalFileName = command.file.originalFilename ?: "template",
                contentType = command.file.contentType ?: "application/octet-stream",
                companyId = command.companyId,
                entityId = UUID.randomUUID().toString(),
                entityType = "template",
                category = "templates",
                subCategory = command.type.name.lowercase(),
                tags = mapOf("templateType" to command.type.name)
            )
        )

        val template = Template(
            id = storageId,
            companyId = command.companyId,
            name = command.name.trim(),
            type = command.type,
            isActive = command.isActive,
            size = command.file.size,
            contentType = command.file.contentType ?: "application/octet-stream",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return templateRepository.save(template)
    }

    fun updateTemplate(command: UpdateTemplateCommand): Template {
        val template = templateRepository.findById(command.templateId, command.companyId)
            ?: throw EntityNotFoundException("Template not found: ${command.templateId}")

        templateValidator.validateTemplateData(command.name, template.type)

        if (command.isActive && !template.isActive) {
            deactivateExistingActiveTemplates(command.companyId, template.type)
        }

        val updatedTemplate = template.updateMetadata(
            name = command.name,
            isActive = command.isActive
        )

        return templateRepository.save(updatedTemplate)
    }

    fun deleteTemplate(templateId: String, companyId: Long) {
        val template = templateRepository.findById(templateId, companyId)
            ?: throw EntityNotFoundException("Template not found: $templateId")

        try {
            storageService.deleteFile(templateId)
        } catch (e: Exception) {
            // Log but don't fail if storage deletion fails
        }

        val deleted = templateRepository.deleteById(templateId, companyId)
        if (!deleted) {
            throw BusinessException("Failed to delete template")
        }
    }

    @Transactional(readOnly = true)
    fun findById(templateId: String, companyId: Long): Template? {
        return templateRepository.findById(templateId, companyId)
    }

    @Transactional(readOnly = true)
    fun findActiveByTypeAndCompany(type: TemplateType, companyId: Long): Template? {
        return templateRepository.findActiveByTypeAndCompany(type, companyId)
    }

    @Transactional(readOnly = true)
    fun findTemplates(
        companyId: Long,
        pageable: Pageable,
        type: TemplateType?,
        isActive: Boolean?
    ): Page<Template> {
        return when {
            type != null && isActive != null -> templateRepository.findByCompanyIdAndTypeAndIsActive(companyId, type, isActive, pageable)
            type != null -> templateRepository.findByCompanyIdAndType(companyId, type, pageable)
            isActive != null -> templateRepository.findByCompanyIdAndIsActive(companyId, isActive, pageable)
            else -> templateRepository.findByCompanyId(companyId, pageable)
        }
    }

    @Transactional(readOnly = true)
    fun getFileData(templateId: String): ByteArray? {
        return templateRepository.getFileData(templateId)
    }

    private fun deactivateExistingActiveTemplates(companyId: Long, type: TemplateType) {
        val activeTemplates = templateRepository.findAllActiveByTypeAndCompany(type, companyId)
        activeTemplates.forEach { activeTemplate ->
            val deactivatedTemplate = activeTemplate.deactivate()
            templateRepository.save(deactivatedTemplate)
        }
    }
}
