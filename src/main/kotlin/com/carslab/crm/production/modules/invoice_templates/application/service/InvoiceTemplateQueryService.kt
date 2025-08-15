package com.carslab.crm.production.modules.invoice_templates.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.invoice_templates.application.dto.InvoiceTemplateHeaderResponse
import com.carslab.crm.production.modules.invoice_templates.application.dto.InvoiceTemplateResponse
import com.carslab.crm.production.modules.invoice_templates.domain.service.InvoiceTemplateDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class InvoiceTemplateQueryService(
    private val domainService: InvoiceTemplateDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(InvoiceTemplateQueryService::class.java)

    fun getTemplatesForCurrentCompany(): List<InvoiceTemplateHeaderResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching templates for company: {}", companyId)

        val templates = domainService.getTemplatesForCompany(companyId)
        logger.debug("Found {} templates for company: {}", templates.size, companyId)

        return templates.map { InvoiceTemplateHeaderResponse.from(it) }
    }
    
    fun findActiveTemplateForCompany(): InvoiceTemplateResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching active template for company: {}", companyId)

        val template = domainService.getActiveTemplateForCompany(companyId)
        logger.debug("Active template found: {}", template.name)

        return InvoiceTemplateResponse(
            header = InvoiceTemplateHeaderResponse.from(template),
            htmlContent = template.htmlContent,
        )
    }

    fun getTemplate(templateId: String): InvoiceTemplateResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching template: {} for company: {}", templateId, companyId)

        val template = domainService.getTemplateForCompany(templateId, companyId)
        logger.debug("Template found: {}", template.name)

        return InvoiceTemplateResponse(
            header = InvoiceTemplateHeaderResponse.from(template),
            htmlContent = template.htmlContent,
        )
    }
}