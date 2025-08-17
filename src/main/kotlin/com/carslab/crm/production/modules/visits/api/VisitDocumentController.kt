package com.carslab.crm.production.modules.visits.api

import com.carslab.crm.modules.visits.api.response.ProtocolDocumentDto
import com.carslab.crm.production.modules.visits.application.dto.UploadDocumentRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitDocumentResponse
import com.carslab.crm.production.modules.visits.application.service.command.VisitDocumentCommandService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDocumentQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Visit Documents", description = "Document operations for visits")
class VisitDocumentController(
    private val visitDocumentCommandService: VisitDocumentCommandService,
    private val visitDocumentQueryService: VisitDocumentQueryService
) {

    @PostMapping("/{visitId}/document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload document to visit")
    fun uploadDocument(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @ModelAttribute request: UploadDocumentRequest
    ): ResponseEntity<ProtocolDocumentDto> {
        val document = visitDocumentCommandService.uploadDocument(visitId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(document)
    }

    @GetMapping("/{visitId}/documents")
    @Operation(summary = "Get visit documents")
    fun getVisitDocuments(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<List<ProtocolDocumentDto>> {
        val documents = visitDocumentQueryService.getVisitDocuments(visitId)
        return ResponseEntity.ok(documents)
    }

    @GetMapping("/document/{documentId}")
    @Operation(summary = "Download document file")
    fun downloadDocument(
        @Parameter(description = "Document ID") @PathVariable documentId: String
    ): ResponseEntity<Resource> {
        val documentData = visitDocumentQueryService.getDocumentFile(documentId)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(documentData)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document_$documentId.pdf\"")
            .body(resource)
    }

    @DeleteMapping("{visitId}/document/{documentId}")
    @Operation(summary = "Download document file")
    fun deleteDocument(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @Parameter(description = "Document ID") @PathVariable documentId: String
    ): ResponseEntity<Void> {
        visitDocumentQueryService.deleteDocument(documentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok().build()
    }
}