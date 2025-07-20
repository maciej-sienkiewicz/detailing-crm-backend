package com.carslab.crm.modules.invoice_templates.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.invoice_templates.api.requests.ActivateTemplateRequest
import com.carslab.crm.modules.invoice_templates.api.requests.UploadTemplateRequest
import com.carslab.crm.modules.invoice_templates.api.responses.InvoiceTemplateResponse
import com.carslab.crm.modules.invoice_templates.domain.InvoiceTemplateService
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplateId
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/invoice-templates")
@Tag(name = "Invoice Templates", description = "Zarządzanie szablonami faktur")
class InvoiceTemplateController(
    private val invoiceTemplateService: InvoiceTemplateService,
    private val securityContext: SecurityContext
) : BaseController() {
    
    @GetMapping
    @Operation(summary = "Pobierz szablony faktur")
    fun getTemplates(): ResponseEntity<List<InvoiceTemplateResponse>> {
        val companyId = getSecureCompanyId()

        logger.debug("Getting templates for company: {}", companyId)
        val templates = invoiceTemplateService.getTemplatesForCompany(companyId)
        val responses = templates.map { InvoiceTemplateResponse.from(it) }

        logger.debug("Found {} templates for company: {}", responses.size, companyId)
        return ok(responses)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pobierz szablon")
    fun getTemplate(@PathVariable id: String): ResponseEntity<InvoiceTemplateResponse> {
        val companyId = getSecureCompanyId()
        val templateId = InvoiceTemplateId(id)

        logger.debug("Getting template {} for company: {}", templateId.value, companyId)

        // Sprawdź uprawnienia w service - rzuci wyjątek jeśli brak dostępu
        val template = invoiceTemplateService.getTemplate(templateId, companyId)
        return ok(InvoiceTemplateResponse.from(template))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Wgraj szablon")
    fun uploadTemplate(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("name") name: String,
        @RequestParam("description", required = false) description: String?
    ): ResponseEntity<InvoiceTemplateResponse> {
        val companyId = getSecureCompanyId()

        logger.debug("Uploading template '{}' for company: {}", name, companyId)

        val request = UploadTemplateRequest(file, name, description)
        val template = invoiceTemplateService.uploadTemplate(request, companyId)

        logger.info("Template '{}' uploaded successfully for company: {}", template.name, companyId)
        return created(InvoiceTemplateResponse.from(template))
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Aktywuj szablon")
    fun activateTemplate(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        val companyId = getSecureCompanyId()
        val templateId = InvoiceTemplateId(id)

        logger.debug("Activating template {} for company: {}", templateId.value, companyId)

        val request = ActivateTemplateRequest(templateId)
        invoiceTemplateService.activateTemplate(companyId, request)

        logger.info("Template {} activated for company: {}", templateId.value, companyId)
        return ok(createSuccessResponse("Template activated", mapOf("templateId" to id)))
    }

    @PostMapping("/{id}/preview")
    @Operation(summary = "Podgląd szablonu")
    fun previewTemplate(@PathVariable id: String): ResponseEntity<ByteArray> {
        val companyId = getSecureCompanyId()
        val templateId = InvoiceTemplateId(id)

        logger.debug("Generating preview for template {} for company: {}", templateId.value, companyId)

        // Sprawdź uprawnienia w service
        val pdfBytes = invoiceTemplateService.generateTemplatePreview(templateId, companyId)

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"template-preview.pdf\"")
            .body(pdfBytes)
    }

    @PostMapping("/documents/{documentId}/generate")
    @Operation(summary = "Wygeneruj fakturę dla dokumentu")
    fun generateInvoiceForDocument(
        @Parameter(description = "ID dokumentu finansowego") @PathVariable documentId: String,
        @Parameter(description = "ID szablonu (opcjonalne)") @RequestParam(required = false) templateId: String?
    ): ResponseEntity<ByteArray> {
        val companyId = getSecureCompanyId()

        logger.debug("Generating invoice for document {} with template {} for company: {}",
            documentId, templateId, companyId)

        val pdfBytes = invoiceTemplateService.generateInvoiceForDocument(
            companyId = companyId,
            documentId = documentId,
            templateId = templateId?.let { InvoiceTemplateId(it) }
        )

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-$documentId.pdf\"")
            .body(pdfBytes)
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Eksportuj szablon")
    fun exportTemplate(@PathVariable id: String): ResponseEntity<ByteArray> {
        val companyId = getSecureCompanyId()
        val templateId = InvoiceTemplateId(id)

        logger.debug("Exporting template {} for company: {}", templateId.value, companyId)

        // Sprawdź uprawnienia w service
        val htmlBytes = invoiceTemplateService.exportTemplate(templateId, companyId)

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"template-${id}.html\"")
            .body(htmlBytes)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Usuń szablon")
    fun deleteTemplate(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        val companyId = getSecureCompanyId()
        val templateId = InvoiceTemplateId(id)

        logger.debug("Deleting template {} for company: {}", templateId.value, companyId)

        val deleted = invoiceTemplateService.deleteTemplate(templateId, companyId)

        return if (deleted) {
            logger.info("Template {} deleted successfully for company: {}", templateId.value, companyId)
            ok(createSuccessResponse("Template deleted", mapOf("templateId" to id)))
        } else {
            logger.warn("Template {} not found for deletion for company: {}", templateId.value, companyId)
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Bezpieczne pobranie company ID z sprawdzeniem autoryzacji
     */
    private fun getSecureCompanyId(): Long {
        return try {
            val companyId = securityContext.getCurrentCompanyId()
            if (companyId <= 0) {
                logger.error("Invalid company ID from security context: {}", companyId)
                throw ValidationException("Invalid company context")
            }
            companyId
        } catch (e: Exception) {
            logger.error("Failed to get company ID from security context", e)
            throw ValidationException("Access denied - invalid company context")
        }
    }
}