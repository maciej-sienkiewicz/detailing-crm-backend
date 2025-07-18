package com.carslab.crm.modules.invoice_templates.api.requests

import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplateId
import org.springframework.web.multipart.MultipartFile

data class UploadTemplateRequest(
    val file: MultipartFile,
    val name: String,
    val description: String?
)

data class ActivateTemplateRequest(
    val templateId: InvoiceTemplateId
)