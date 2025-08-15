package com.carslab.crm.production.modules.invoice_templates.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.production.modules.invoice_templates.domain.service.InvoiceTemplateDomainService
import com.carslab.crm.production.modules.invoice_templates.domain.service.PdfGenerationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class InvoiceGenerationService(
    private val domainService: InvoiceTemplateDomainService,
    private val pdfGenerationService: PdfGenerationService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(InvoiceGenerationService::class.java)

    fun generatePreview(templateId: String): ByteArray {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Generating preview for template: {} for company: {}", templateId, companyId)

        val template = domainService.getTemplateForCompany(templateId, companyId)
        val pdf = pdfGenerationService.generatePreview(template)

        logger.debug("Preview generated successfully, size: {} bytes", pdf.size)
        return pdf
    }

    fun generateInvoice(documentId: String, templateId: String?): ByteArray {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Generating invoice for document: {} with template: {} for company: {}",
            documentId, templateId, companyId)

        val template = fetchTemplateDefinition(templateId, companyId)
        val pdf = pdfGenerationService.generateInvoice(template, documentId, companyId)

        logger.info("Invoice generated successfully for document: {}, size: {} bytes", documentId, pdf.size)
        return pdf
    }

    private fun fetchTemplateDefinition(templateId: String?, companyId: Long): InvoiceTemplate =
        when (templateId) {
            null -> domainService.getActiveTemplateForCompany(companyId)
            else -> domainService.getTemplateForCompany(templateId, companyId)
        }
}