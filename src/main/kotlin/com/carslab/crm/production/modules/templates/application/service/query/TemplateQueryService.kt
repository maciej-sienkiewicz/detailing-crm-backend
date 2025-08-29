package com.carslab.crm.production.modules.templates.application.service.query

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.templates.application.dto.TemplateDownloadResponse
import com.carslab.crm.production.modules.templates.application.dto.TemplateResponse
import com.carslab.crm.production.modules.templates.application.dto.TemplateTypeResponse
import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import com.carslab.crm.production.modules.templates.domain.service.TemplateService
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TemplateQueryService(
    private val templateService: TemplateService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(TemplateQueryService::class.java)

    fun getTemplates(
        pageable: Pageable,
        type: String?,
        isActive: Boolean?
    ): PaginatedResponse<TemplateResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching templates for company: {}", companyId)

        val templateType = type?.let { TemplateType.fromString(it) }
        val templates = templateService.findTemplates(companyId, pageable, templateType, isActive)

        val responses = templates.content.map { TemplateResponse.from(it) }

        return PaginatedResponse(
            data = responses,
            page = templates.number,
            size = templates.size,
            totalItems = templates.totalElements,
            totalPages = templates.totalPages.toLong()
        )
    }

    fun getTemplateTypes(): List<TemplateTypeResponse> {
        return TemplateType.entries.map { type ->
            TemplateTypeResponse(
                type = type
            )
        }
    }

    fun downloadTemplate(templateId: String): TemplateDownloadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Downloading template: {} for company: {}", templateId, companyId)

        val template = templateService.findById(templateId, companyId)
            ?: throw EntityNotFoundException("Template not found: $templateId")

        val fileData = templateService.getFileData(templateId)
            ?: throw EntityNotFoundException("Template file data not found: $templateId")

        val resource = ByteArrayResource(fileData)

        return TemplateDownloadResponse(
            resource = resource,
            contentType = template.contentType,
            originalName = template.name
        )
    }

    fun findActiveTemplateByTemplateType(type: TemplateType, companyId: Long): TemplateResponse? {
        logger.debug("Finding active template of type: {} for company: {}", type, companyId)

        val template = templateService.findActiveByTypeAndCompany(type, companyId)
        return template?.let { TemplateResponse.from(it) }
    }
}