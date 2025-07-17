package com.carslab.crm.modules.invoice_templates.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.invoice_templates.api.responses.InvoiceTemplateResponse
import com.carslab.crm.modules.invoice_templates.domain.InvoiceTemplateService
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplateId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
        val companyId = securityContext.getCurrentCompanyId()
        val templates = invoiceTemplateService.getTemplatesForCompany(companyId)
        val responses = templates.map { InvoiceTemplateResponse.from(it) }
        return ok(responses)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pobierz szablon")
    fun getTemplate(@PathVariable id: String): ResponseEntity<InvoiceTemplateResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val template = invoiceTemplateService.getTemplate(InvoiceTemplateId(id), companyId)
        return ok(InvoiceTemplateResponse.from(template))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Wgraj szablon")
    fun uploadTemplate(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("name") name: String,
        @RequestParam("description", required = false) description: String?
    ): ResponseEntity<InvoiceTemplateResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val template = invoiceTemplateService.uploadTemplate(file, companyId, name, description)
        return created(InvoiceTemplateResponse.from(template))
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Aktywuj szablon")
    fun activateTemplate(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()
        invoiceTemplateService.activateTemplate(companyId, InvoiceTemplateId(id))
        return ok(createSuccessResponse("Template activated", mapOf("templateId" to id)))
    }

    @PostMapping("/{id}/preview")
    @Operation(summary = "Podgląd szablonu")
    fun previewTemplate(@PathVariable id: String): ResponseEntity<ByteArray> {
        val companyId = securityContext.getCurrentCompanyId()
        val pdfBytes = invoiceTemplateService.generateTemplatePreview(InvoiceTemplateId(id), companyId)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "inline; filename=\"template-preview.pdf\"")
            .body(pdfBytes)
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Eksportuj szablon")
    fun exportTemplate(@PathVariable id: String): ResponseEntity<ByteArray> {
        val companyId = securityContext.getCurrentCompanyId()
        val htmlBytes = invoiceTemplateService.exportTemplate(InvoiceTemplateId(id), companyId)
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .header("Content-Disposition", "attachment; filename=\"template-${id}.html\"")
            .body(htmlBytes)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Usuń szablon")
    fun deleteTemplate(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()
        val deleted = invoiceTemplateService.deleteTemplate(InvoiceTemplateId(id), companyId)
        return if (deleted) {
            ok(createSuccessResponse("Template deleted", mapOf("templateId" to id)))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}