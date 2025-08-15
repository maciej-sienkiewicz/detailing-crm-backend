package com.carslab.crm.production.modules.invoice_templates.domain.service

import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplate

interface PdfGenerationService {
    fun generatePreview(template: InvoiceTemplate): ByteArray
    fun generateInvoice(template: InvoiceTemplate, documentId: String, companyId: Long): ByteArray
}