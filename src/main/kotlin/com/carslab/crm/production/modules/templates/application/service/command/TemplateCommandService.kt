package com.carslab.crm.production.modules.templates.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.templates.application.dto.TemplateResponse
import com.carslab.crm.production.modules.templates.application.dto.UpdateTemplateRequest
import com.carslab.crm.production.modules.templates.application.dto.UploadTemplateRequest
import com.carslab.crm.production.modules.templates.domain.command.CreateTemplateCommand
import com.carslab.crm.production.modules.templates.domain.command.UpdateTemplateCommand
import com.carslab.crm.production.modules.templates.domain.service.TemplateService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TemplateCommandService(
    private val templateService: TemplateService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(TemplateCommandService::class.java)

    fun uploadTemplate(request: UploadTemplateRequest): TemplateResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading template '{}' for company: {}", request.name, companyId)

        val command = CreateTemplateCommand(
            companyId = companyId,
            file = request.file,
            name = request.name,
            type = request.type,
            isActive = request.isActive
        )

        val template = templateService.createTemplate(command)
        logger.info("Template uploaded successfully: {}", template.id)

        return TemplateResponse.from(template)
    }

    fun updateTemplate(templateId: String, request: UpdateTemplateRequest): TemplateResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating template: {} for company: {}", templateId, companyId)

        val command = UpdateTemplateCommand(
            templateId = templateId,
            name = request.name,
            isActive = request.isActive,
            companyId = companyId
        )

        val template = templateService.updateTemplate(command)
        logger.info("Template updated successfully: {}", templateId)

        return TemplateResponse.from(template)
    }

    fun deleteTemplate(templateId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting template: {} for company: {}", templateId, companyId)

        templateService.deleteTemplate(templateId, companyId)
        logger.info("Template deleted successfully: {}", templateId)
    }
}