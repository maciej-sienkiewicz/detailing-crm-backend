package com.carslab.crm.modules.invoice_templates.domain.ports

import com.carslab.crm.modules.invoice_templates.domain.model.LayoutSettings

interface PdfGenerationService {
    fun generatePdf(html: String): ByteArray
    fun validateHtmlForPdf(html: String): Boolean
}