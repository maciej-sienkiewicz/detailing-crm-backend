package com.carslab.crm.production.modules.visits.api

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.visits.api.commands.CarReceptionDetailDto
import com.carslab.crm.modules.visits.application.queries.models.VisitListReadModel
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.application.service.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.application.service.VisitCommandService
import com.carslab.crm.production.modules.visits.application.service.VisitQueryService
import com.carslab.crm.production.modules.visits.application.service.VisitListQueryService
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Visit Management", description = "Endpoints for managing visits")
class VisitController(
    private val visitCommandService: VisitCommandService,
    private val visitQueryService: VisitQueryService,
    private val visitListQueryService: VisitListQueryService,
    private val visitDetailQueryService: VisitDetailQueryService,
) {

    @PostMapping
    @Operation(summary = "Create new visit")
    fun createVisit(@Valid @RequestBody request: CreateVisitRequest): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.createVisit(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(visit)
    }

    @GetMapping("/{visitId}")
    @Operation(summary = "Get visit by ID")
    fun getVisit(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<CarReceptionDetailDto> {
        val visit = visitDetailQueryService.getVisitDetail(visitId)
        return ResponseEntity.ok(visit)
    }

    @PutMapping("/{visitId}")
    @Operation(summary = "Update visit")
    fun updateVisit(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @Valid @RequestBody request: UpdateVisitRequest
    ): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.updateVisit(visitId, request)
        return ResponseEntity.ok(visit)
    }

    @PatchMapping("/{visitId}/status")
    @Operation(summary = "Change visit status")
    fun changeVisitStatus(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @Valid @RequestBody request: ChangeStatusRequest
    ): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.changeVisitStatus(visitId, request)
        return ResponseEntity.ok(visit)
    }

    @DeleteMapping("/{visitId}")
    @Operation(summary = "Delete visit")
    fun deleteVisit(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<Void> {
        visitCommandService.deleteVisit(visitId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/list")
    @Operation(summary = "Get visits with pagination")
    fun getVisits(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") sortDirection: String
    ): ResponseEntity<PaginatedResponse<VisitListReadModel>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy)
        val pageable = PageRequest.of(page, size, sort)
        val visits = visitListQueryService.getVisitList(pageable)
        return ResponseEntity.ok(visits)
    }

    @GetMapping("/counters")
    @Operation(summary = "Get visit counters by status")
    fun getVisitCounters(): ResponseEntity<VisitCountersResponse> {
        val counters = visitQueryService.getVisitCounters()
        return ResponseEntity.ok(counters)
    }

    @PostMapping("/{visitId}/comments")
    @Operation(summary = "Add comment to visit")
    fun addComment(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @Valid @RequestBody request: AddCommentRequest
    ): ResponseEntity<VisitCommentResponse> {
        val comment = visitCommandService.addComment(visitId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(comment)
    }

    @GetMapping("/{visitId}/comments")
    @Operation(summary = "Get visit comments")
    fun getVisitComments(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<List<VisitCommentResponse>> {
        val comments = visitQueryService.getVisitComments(visitId)
        return ResponseEntity.ok(comments)
    }

    @PostMapping("/{visitId}/media", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload media to visit")
    fun uploadMedia(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @ModelAttribute request: UploadMediaRequest
    ): ResponseEntity<VisitMediaResponse> {
        val media = visitCommandService.uploadMedia(visitId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(media)
    }

    @GetMapping("/{visitId}/media")
    @Operation(summary = "Get visit media")
    fun getVisitMedia(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<List<VisitMediaResponse>> {
        val media = visitQueryService.getVisitMedia(visitId)
        return ResponseEntity.ok(media)
    }

    @GetMapping("/media/{mediaId}/download")
    @Operation(summary = "Download media file")
    fun downloadMedia(
        @Parameter(description = "Media ID") @PathVariable mediaId: String
    ): ResponseEntity<Resource> {
        val mediaData = visitQueryService.getMediaFile(mediaId)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(mediaData)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"media_$mediaId\"")
            .body(resource)
    }

    @PostMapping("/{visitId}/documents", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload document to visit")
    fun uploadDocument(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @ModelAttribute request: UploadDocumentRequest
    ): ResponseEntity<VisitDocumentResponse> {
        val document = visitCommandService.uploadDocument(visitId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(document)
    }

    @GetMapping("/{visitId}/documents")
    @Operation(summary = "Get visit documents")
    fun getVisitDocuments(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<List<VisitDocumentResponse>> {
        val documents = visitQueryService.getVisitDocuments(visitId)
        return ResponseEntity.ok(documents)
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "Download document file")
    fun downloadDocument(
        @Parameter(description = "Document ID") @PathVariable documentId: String
    ): ResponseEntity<Resource> {
        val documentData = visitQueryService.getDocumentFile(documentId)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(documentData)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document_$documentId.pdf\"")
            .body(resource)
    }

    @GetMapping("/clients/{clientId}")
    @Operation(summary = "Get visits for specific client")
    fun getVisitsForClient(
        @Parameter(description = "Client ID") @PathVariable clientId: String,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<VisitResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val visits = visitQueryService.getVisitsForClient(clientId, pageable)
        return ResponseEntity.ok(visits)
    }

    @GetMapping("/vehicles/{vehicleId}")
    @Operation(summary = "Get visits for specific vehicle")
    fun getVisitsForVehicle(
        @Parameter(description = "Vehicle ID") @PathVariable vehicleId: String,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<VisitResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val visits = visitQueryService.getVisitsForVehicle(vehicleId, pageable)
        return ResponseEntity.ok(visits)
    }
}