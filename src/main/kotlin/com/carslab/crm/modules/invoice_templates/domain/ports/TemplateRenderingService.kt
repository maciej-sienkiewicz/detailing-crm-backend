package com.carslab.crm.modules.invoice_templates.domain.ports

import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData
import com.carslab.crm.production.modules.invoice_templates.application.dto.InvoiceTemplateResponse

interface TemplateRenderingService {
    fun renderTemplate(template: InvoiceTemplateResponse, data: InvoiceGenerationData): String
    fun validateTemplateSyntax(htmlTemplate: String): Boolean
}