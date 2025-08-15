package com.carslab.crm.production.modules.invoice_templates.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.invoice_templates.application.dto.InvoiceTemplateHeaderResponse
import com.carslab.crm.production.modules.invoice_templates.application.dto.UploadTemplateRequest
import com.carslab.crm.production.modules.invoice_templates.domain.command.CreateTemplateCommand
import com.carslab.crm.production.modules.invoice_templates.domain.service.InvoiceTemplateDomainService
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class InvoiceTemplateCommandService(
    private val domainService: InvoiceTemplateDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(InvoiceTemplateCommandService::class.java)

    fun uploadTemplate(request: UploadTemplateRequest): InvoiceTemplateHeaderResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading template '{}' for company: {}", request.name, companyId)

        validateUploadRequest(request)
        val htmlContent = extractHtmlContent(request)

        val command = CreateTemplateCommand(
            companyId = companyId,
            name = request.name.trim(),
            description = request.description?.trim(),
            htmlContent = htmlContent
        )

        val template = domainService.createTemplate(command)
        logger.info("Template created successfully: {}", template.id.value)

        return InvoiceTemplateHeaderResponse.from(template)
    }

    fun activateTemplate(templateId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Activating template: {} for company: {}", templateId, companyId)

        domainService.activateTemplate(templateId, companyId)
        logger.info("Template activated successfully: {}", templateId)
    }

    fun deleteTemplate(templateId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting template: {} for company: {}", templateId, companyId)

        val deleted = domainService.deleteTemplate(templateId, companyId)
        if (deleted) {
            logger.info("Template deleted successfully: {}", templateId)
        } else {
            logger.warn("Template not found for deletion: {}", templateId)
        }
    }

    private fun validateUploadRequest(request: UploadTemplateRequest) {
        if (request.file.isEmpty) {
            throw BusinessException("File cannot be empty")
        }

        if (request.file.size > 5 * 1024 * 1024) {
            throw BusinessException("File size cannot exceed 5MB")
        }

        val filename = request.file.originalFilename
        if (filename == null || !filename.endsWith(".html", ignoreCase = true)) {
            throw BusinessException("File must be HTML format")
        }

        logger.debug("Upload request validated for file: {}", filename)
    }

    private fun extractHtmlContent(request: UploadTemplateRequest): String {
        return try {
            val content = String(request.file.bytes, Charsets.UTF_8)
            if (content.isBlank()) {
                throw BusinessException("HTML content cannot be empty")
            }
            logger.debug("HTML content extracted, length: {}", content.length)
            content
        } catch (e: Exception) {
            logger.error("Failed to extract HTML content", e)
            throw BusinessException("Failed to read HTML content: ${e.message}")
        }
    }
}