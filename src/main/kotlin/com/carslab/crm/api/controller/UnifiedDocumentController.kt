package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.*
import com.carslab.crm.api.model.request.CreateUnifiedDocumentRequest
import com.carslab.crm.api.model.request.UpdateUnifiedDocumentRequest
import com.carslab.crm.api.model.response.UnifiedDocumentResponse
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.domain.finances.documents.UnifiedDocumentService
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

@RestController
@RequestMapping("/api/financial-documents")
@Tag(name = "Financial Documents", description = "API endpoints for unified financial document management")
class UnifiedDocumentController(
    private val documentService: UnifiedDocumentService,
    private val objectMapper: ObjectMapper,
) : BaseController() {

    @GetMapping
    @Operation(summary = "Get all financial documents", description = "Retrieves all documents with optional filtering and pagination")
    fun getAllDocuments(
        @Parameter(description = "Document number") @RequestParam(required = false) number: String?,
        @Parameter(description = "Document title") @RequestParam(required = false) title: String?,
        @Parameter(description = "Buyer name") @RequestParam(required = false) buyerName: String?,
        @Parameter(description = "Seller name") @RequestParam(required = false) sellerName: String?,
        @Parameter(description = "Document status") @RequestParam(required = false) status: String?,
        @Parameter(description = "Document type") @RequestParam(required = false) type: String?,
        @Parameter(description = "Transaction direction") @RequestParam(required = false) direction: String?,
        @Parameter(description = "Payment method") @RequestParam(required = false) paymentMethod: String?,
        @Parameter(description = "Issue date from") @RequestParam(required = false) dateFrom: String?,
        @Parameter(description = "Issue date to") @RequestParam(required = false) dateTo: String?,
        @Parameter(description = "Protocol ID") @RequestParam(required = false) protocolId: String?,
        @Parameter(description = "Visit ID") @RequestParam(required = false) visitId: String?,
        @Parameter(description = "Minimum amount") @RequestParam(required = false) minAmount: BigDecimal?,
        @Parameter(description = "Maximum amount") @RequestParam(required = false) maxAmount: BigDecimal?,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PaginatedResponse<UnifiedDocumentResponse>> {
        logger.info("Getting financial documents with filters")

        val filter = UnifiedDocumentFilterDTO(
            number = number,
            title = title,
            buyerName = buyerName,
            sellerName = sellerName,
            status = status,
            type = type,
            direction = direction,
            paymentMethod = paymentMethod,
            dateFrom = dateFrom?.let { java.time.LocalDate.parse(it) },
            dateTo = dateTo?.let { java.time.LocalDate.parse(it) },
            protocolId = protocolId,
            visitId = visitId,
            minAmount = minAmount,
            maxAmount = maxAmount
        )

        val documents = documentService.getAllDocuments(filter, page, size)
        val response = PaginatedResponse(
            data = documents.data.map { it.toResponse() },
            page = documents.page,
            size = documents.size,
            totalItems = documents.totalItems,
            totalPages = documents.totalPages.toLong()
        )

        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID", description = "Retrieves a document by its ID")
    fun getDocumentById(
        @Parameter(description = "Document ID", required = true) @PathVariable id: String
    ): ResponseEntity<UnifiedDocumentResponse> {
        logger.info("Getting document by ID: {}", id)

        val document = documentService.getDocumentById(id)
        val response = document.toResponse()

        return ok(response)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Create a new document", description = "Creates a new financial document with optional attachment")
    fun createDocument(
        @Parameter(description = "Document data", required = true)
        @RequestPart("document") @Valid document: String,
        @Parameter(description = "Document attachment")
        @RequestPart("attachment", required = false) attachment: MultipartFile?
    ): ResponseEntity<UnifiedDocumentResponse> {
        logger.info("Creating new financial document")

        val createRequest = objectMapper.readValue(document, CreateUnifiedDocumentRequest::class.java)
        logger.info("Document data: {}", createRequest)

        val createdDocument = documentService.createDocument(createRequest, attachment)
        val response = createdDocument.toResponse()

        return created(response)
    }

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Update a document", description = "Updates an existing document with optional attachment")
    fun updateDocument(
        @Parameter(description = "Document ID", required = true) @PathVariable id: String,
        @Parameter(description = "Document data", required = true)
        @RequestPart("document") @Valid document: String,
        @Parameter(description = "Document attachment")
        @RequestPart("attachment", required = false) attachment: MultipartFile?
    ): ResponseEntity<UnifiedDocumentResponse> {
        logger.info("Updating document with ID: {}", id)

        val updateRequest = objectMapper.readValue(document, UpdateUnifiedDocumentRequest::class.java)

        val updatedDocument = documentService.updateDocument(id, updateRequest, attachment)
        val response = updatedDocument.toResponse()

        return ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document", description = "Deletes a document by its ID")
    fun deleteDocument(
        @Parameter(description = "Document ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting document with ID: {}", id)

        val deleted = documentService.deleteDocument(id)

        return if (deleted) {
            ok(createSuccessResponse("Document successfully deleted", mapOf("documentId" to id)))
        } else {
            badRequest("Failed to delete document")
        }
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update document status", description = "Updates the status of a document")
    fun updateDocumentStatus(
        @Parameter(description = "Document ID", required = true) @PathVariable id: String,
        @RequestBody statusRequest: StatusUpdateRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Updating status of document with ID: {} to: {}", id, statusRequest.status)

        val updated = documentService.updateDocumentStatus(id, statusRequest.status)

        return if (updated) {
            ok(createSuccessResponse("Document status successfully updated",
                mapOf("documentId" to id, "status" to statusRequest.status)))
        } else {
            badRequest("Failed to update document status")
        }
    }

    @PatchMapping("/{id}/paid")
    @Operation(summary = "Update paid amount", description = "Updates the paid amount of a document")
    fun updatePaidAmount(
        @Parameter(description = "Document ID", required = true) @PathVariable id: String,
        @Parameter(description = "Paid amount", required = true) @RequestParam paidAmount: BigDecimal
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Updating paid amount of document with ID: {} to: {}", id, paidAmount)

        val updated = documentService.updatePaidAmount(id, paidAmount)

        return if (updated) {
            ok(createSuccessResponse("Document paid amount successfully updated",
                mapOf("documentId" to id, "paidAmount" to paidAmount)))
        } else {
            badRequest("Failed to update document paid amount")
        }
    }

    @GetMapping("/{id}/attachment")
    @Operation(summary = "Get document attachment", description = "Downloads the attachment of a document")
    fun getDocumentAttachment(
        @Parameter(description = "Document ID", required = true) @PathVariable id: String
    ): ResponseEntity<Resource> {
        logger.info("Getting attachment for document with ID: {}", id)

        val attachmentData = documentService.getDocumentAttachment(id)
            ?: return ResponseEntity.notFound().build()

        val (fileBytes, contentType) = attachmentData
        val resource = ByteArrayResource(fileBytes)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document-$id\"")
            .body(resource)
    }

    @PostMapping("/extract")
    @Operation(summary = "Extract document data", description = "Extracts data from a document file")
    fun extractDocumentData(
        @Parameter(description = "Document file", required = true) @RequestPart("file") file: MultipartFile
    ): ResponseEntity<DocumentDataResponse> {
        logger.info("Extracting data from document file: {}", file.originalFilename)

        val extractedData = documentService.extractDocumentData(file)
        val response = DocumentDataResponse(extractedDocumentData = extractedData)

        return ok(response)
    }

    @GetMapping("/summary")
    @Operation(summary = "Get financial summary", description = "Gets financial summary for specified period")
    fun getFinancialSummary(
        @Parameter(description = "Date from") @RequestParam(required = false) dateFrom: String?,
        @Parameter(description = "Date to") @RequestParam(required = false) dateTo: String?
    ): ResponseEntity<FinancialSummaryResponse> {
        logger.info("Getting financial summary for period: {} to {}", dateFrom, dateTo)

        val summary = documentService.getFinancialSummary(
            dateFrom?.let { java.time.LocalDate.parse(it) },
            dateTo?.let { java.time.LocalDate.parse(it) }
        )

        return ok(summary)
    }

    @GetMapping("/chart-data")
    @Operation(summary = "Get chart data", description = "Gets financial chart data for specified period")
    fun getChartData(
        @Parameter(description = "Period") @RequestParam(defaultValue = "month") period: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Getting chart data for period: {}", period)

        val chartData = documentService.getChartData(period)

        return ok(chartData)
    }

    // Funkcja pomocnicza do konwersji modelu domenowego na DTO odpowiedzi
    private fun UnifiedFinancialDocument.toResponse(): UnifiedDocumentResponse {
        return UnifiedDocumentResponse(
            id = id.value,
            number = number,
            type = type.name,
            title = title,
            description = description,
            issuedDate = issuedDate,
            dueDate = dueDate,
            sellerName = sellerName,
            sellerTaxId = sellerTaxId,
            sellerAddress = sellerAddress,
            buyerName = buyerName,
            buyerTaxId = buyerTaxId,
            buyerAddress = buyerAddress,
            status = status.name,
            direction = direction.name,
            paymentMethod = paymentMethod.name,
            totalNet = totalNet,
            totalTax = totalTax,
            totalGross = totalGross,
            paidAmount = paidAmount,
            currency = currency,
            notes = notes,
            protocolId = protocolId,
            protocolNumber = protocolNumber,
            visitId = visitId,
            createdAt = audit.createdAt,
            updatedAt = audit.updatedAt,
            items = items.map { item ->
                DocumentItemDTO(
                    id = item.id,
                    name = item.name,
                    description = item.description,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    taxRate = item.taxRate,
                    totalNet = item.totalNet,
                    totalGross = item.totalGross
                )
            },
            attachments = attachment?.let {
                listOf(DocumentAttachmentDTO(
                    id = it.id,
                    name = it.name,
                    size = it.size,
                    type = it.type,
                    uploadedAt = it.uploadedAt
                ))
            } ?: emptyList()
        )
    }
}

data class StatusUpdateRequest(@JsonProperty("status") var status: String)