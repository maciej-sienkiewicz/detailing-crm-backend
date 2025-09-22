package com.carslab.crm.production.modules.templates.api

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.production.modules.templates.application.dto.*
import com.carslab.crm.production.modules.templates.application.service.command.TemplateCommandService
import com.carslab.crm.production.modules.templates.application.service.query.TemplateQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates", description = "Template file management operations")
class TemplateController(
    private val templateCommandService: TemplateCommandService,
    private val templateQueryService: TemplateQueryService
) {

    @GetMapping
    @Operation(summary = "Get all templates with pagination")
    fun getTemplates(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") sortDirection: String,
        @Parameter(description = "Template type") @RequestParam(required = false) type: String?,
        @Parameter(description = "Active status") @RequestParam(required = false) isActive: Boolean?
    ): ResponseEntity<PaginatedResponse<TemplateResponse>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val templates = templateQueryService.getTemplates(pageable, type, isActive)
        return ResponseEntity.ok(templates)
    }

    @GetMapping("/types")
    @Operation(summary = "Get available template types")
    fun getTemplateTypes(): ResponseEntity<List<TemplateTypeResponse>> {
        val types = templateQueryService.getTemplateTypes()
        return ResponseEntity.ok(types)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload new template")
    fun uploadTemplate(
        @ModelAttribute @Valid request: UploadTemplateRequest
    ): ResponseEntity<TemplateResponse> {
        val template = templateCommandService.uploadTemplate(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(template)
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "Update template metadata")
    fun updateTemplate(
        @Parameter(description = "Template ID") @PathVariable templateId: String,
        @Valid @RequestBody request: UpdateTemplateRequest
    ): ResponseEntity<TemplateResponse> {
        val template = templateCommandService.updateTemplate(templateId, request)
        return ResponseEntity.ok(template)
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "Delete template")
    fun deleteTemplate(
        @Parameter(description = "Template ID") @PathVariable templateId: String
    ): ResponseEntity<Void> {
        templateCommandService.deleteTemplate(templateId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{templateId}/download")
    @Operation(summary = "Download template file")
    fun downloadTemplate(
        @Parameter(description = "Template ID") @PathVariable templateId: String
    ): ResponseEntity<Resource> {
        val templateFile = templateQueryService.downloadTemplate(templateId)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(templateFile.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${templateFile.originalName}\"")
            .body(templateFile.resource)
    }

    @GetMapping("/{templateId}/preview")
    @Operation(summary = "Preview template file")
    fun previewTemplate(
        @Parameter(description = "Template ID") @PathVariable templateId: String
    ): ResponseEntity<Resource> {
        val templateFile = templateQueryService.downloadTemplate(templateId)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(templateFile.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${templateFile.originalName}\"")
            .body(templateFile.resource)
    }
}