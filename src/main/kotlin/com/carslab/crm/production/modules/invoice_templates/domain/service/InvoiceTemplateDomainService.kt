package com.carslab.crm.production.modules.invoice_templates.domain.service

import com.carslab.crm.production.modules.invoice_templates.domain.command.CreateTemplateCommand
import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplateId
import com.carslab.crm.production.modules.invoice_templates.domain.repository.InvoiceTemplateRepository
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.InvoiceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class InvoiceTemplateDomainService(
    private val repository: InvoiceTemplateRepository,
    private val defaultTemplateProvider: DefaultTemplateProvider
) {
    private val logger = LoggerFactory.getLogger(InvoiceTemplateDomainService::class.java)

    fun createTemplate(command: CreateTemplateCommand): InvoiceTemplate {
        logger.debug("Creating template '{}' for company: {}", command.name, command.companyId)

        if (repository.existsByCompanyIdAndName(command.companyId, command.name)) {
            throw BusinessException("Template with name '${command.name}' already exists")
        }

        val template = InvoiceTemplate(
            id = InvoiceTemplateId.generate(),
            companyId = command.companyId,
            name = command.name,
            description = command.description,
            htmlContent = command.htmlContent,
            isActive = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 0
        )

        val savedTemplate = repository.save(template)
        logger.info("Template created: {} for company: {}", savedTemplate.id.value, command.companyId)
        return savedTemplate
    }

    fun getTemplatesForCompany(companyId: Long): List<InvoiceTemplate> {
        logger.debug("Fetching templates for company: {}", companyId)

        val templates = repository.findByCompanyId(companyId)

        return if (templates.isEmpty()) {
            logger.debug("No templates found for company: {}, creating default", companyId)
            val defaultTemplate = createDefaultTemplate(companyId)
            listOf(defaultTemplate)
        } else {
            logger.debug("Found {} templates for company: {}", templates.size, companyId)
            templates
        }
    }

    fun getTemplateForCompany(templateId: String, companyId: Long): InvoiceTemplate {
        logger.debug("Fetching template: {} for company: {}", templateId, companyId)

        val template = repository.findById(InvoiceTemplateId.of(templateId))
            ?: throw InvoiceNotFoundException("Template not found: $templateId")

        if (!template.canBeUsedBy(companyId)) {
            logger.warn("Access denied to template: {} for company: {}", templateId, companyId)
            throw BusinessException("Access denied to template")
        }

        return template
    }

    fun getActiveTemplateForCompany(companyId: Long): InvoiceTemplate {
        logger.debug("Fetching active template for company: {}", companyId)

        return repository.findActiveTemplateForCompany(companyId)
            ?: createDefaultTemplate(companyId)
    }

    fun activateTemplate(templateId: String, companyId: Long) {
        logger.debug("Activating template: {} for company: {}", templateId, companyId)

        val template = getTemplateForCompany(templateId, companyId)

        repository.deactivateAllForCompany(companyId)
        repository.save(template.activate())

        logger.info("Template activated: {} for company: {}", templateId, companyId)
    }

    fun deleteTemplate(templateId: String, companyId: Long): Boolean {
        logger.debug("Deleting template: {} for company: {}", templateId, companyId)

        val template = getTemplateForCompany(templateId, companyId)
        val deleted = repository.deleteById(template.id)

        if (deleted) {
            logger.info("Template deleted: {} for company: {}", templateId, companyId)
        }

        return deleted
    }

    private fun createDefaultTemplate(companyId: Long): InvoiceTemplate {
        logger.debug("Creating default template for company: {}", companyId)

        val command = CreateTemplateCommand(
            companyId = companyId,
            name = "Default Template",
            description = "Default invoice template",
            htmlContent = defaultTemplateProvider.getDefaultHtmlTemplate()
        )

        val template = InvoiceTemplate(
            id = InvoiceTemplateId.generate(),
            companyId = command.companyId,
            name = command.name,
            description = command.description,
            htmlContent = command.htmlContent,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 0
        )

        val savedTemplate = repository.save(template)
        logger.info("Default template created: {} for company: {}", savedTemplate.id.value, companyId)
        return savedTemplate
    }
}