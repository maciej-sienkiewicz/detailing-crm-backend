package com.carslab.crm.modules.invoice_templates.domain.ports

import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData

interface TemplateRenderingService {
    fun renderTemplate(template: InvoiceTemplate, data: InvoiceGenerationData): String
    fun validateTemplateSyntax(htmlTemplate: String): Boolean
}