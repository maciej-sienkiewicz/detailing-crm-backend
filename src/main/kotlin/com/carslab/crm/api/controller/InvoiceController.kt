package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.*
import com.carslab.crm.api.model.request.CreateInvoiceRequest
import com.carslab.crm.api.model.request.UpdateInvoiceRequest
import com.carslab.crm.api.model.response.InvoiceResponse
import com.carslab.crm.domain.finances.invoices.InvoiceService
import com.carslab.crm.domain.model.view.finance.Invoice
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
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "API endpoints for invoice management")
class InvoiceController(
    private val invoiceService: InvoiceService,
    private val invoiceObjectMapper: ObjectMapper,
) : BaseController() {

    @GetMapping
    @Operation(summary = "Get all invoices", description = "Retrieves all invoices with optional filtering")
    fun getAllInvoices(
        @Parameter(description = "Invoice number") @RequestParam(required = false) number: String?,
        @Parameter(description = "Invoice title") @RequestParam(required = false) title: String?,
        @Parameter(description = "Buyer name") @RequestParam(required = false) buyerName: String?,
        @Parameter(description = "Invoice status") @RequestParam(required = false) status: String?,
        @Parameter(description = "Invoice type") @RequestParam(required = false) type: String?,
        @Parameter(description = "Issue date from") @RequestParam(required = false) dateFrom: String?,
        @Parameter(description = "Issue date to") @RequestParam(required = false) dateTo: String?,
        @Parameter(description = "Protocol ID") @RequestParam(required = false) protocolId: String?,
        @Parameter(description = "Minimum amount") @RequestParam(required = false) minAmount: BigDecimal?,
        @Parameter(description = "Maximum amount") @RequestParam(required = false) maxAmount: BigDecimal?
    ): ResponseEntity<List<InvoiceResponse>> {
        logger.info("Getting all invoices with filters")

        val filter = InvoiceFilterDTO(
            number = number,
            title = title,
            buyerName = buyerName,
            status = status,
            type = type,
            dateFrom = dateFrom?.let { java.time.LocalDate.parse(it) },
            dateTo = dateTo?.let { java.time.LocalDate.parse(it) },
            protocolId = protocolId,
            minAmount = minAmount,
            maxAmount = maxAmount
        )

        val invoices = invoiceService.getAllInvoices(filter)
        val response = invoices.map { it.toResponse() }

        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID", description = "Retrieves an invoice by its ID")
    fun getInvoiceById(
        @Parameter(description = "Invoice ID", required = true) @PathVariable id: String
    ): ResponseEntity<InvoiceResponse> {
        logger.info("Getting invoice by ID: {}", id)

        val invoice = invoiceService.getInvoiceById(id)
        val response = invoice.toResponse()

        return ok(response)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Create a new invoice", description = "Creates a new invoice with optional attachment")
    fun createInvoice(
        @Parameter(description = "Invoice data", required = true)
        @RequestPart("invoice") @Valid invoice: String,
        @Parameter(description = "Invoice attachment")
        @RequestPart("attachment", required = false) attachment: MultipartFile?
    ): ResponseEntity<InvoiceResponse> {
        logger.info("Creating new invoice from JSON string")

        // Convert string to CreateInvoiceRequest
        val createRequest = invoiceObjectMapper.readValue(invoice, CreateInvoiceRequest::class.java)

        // Validate manually since we're not using automatic binding
        logger.info("Invoice data: {}", createRequest)

        val createdInvoice = invoiceService.createInvoice(createRequest, attachment)
        val response = createdInvoice.toResponse()

        return created(response)
    }

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Update an invoice", description = "Updates an existing invoice with optional attachment")
    fun updateInvoice(
        @Parameter(description = "Invoice ID", required = true) @PathVariable id: String,
        @Parameter(description = "Invoice data", required = true)
        @RequestPart("invoice") @Valid invoice: String,
        @Parameter(description = "Invoice attachment")
        @RequestPart("attachment", required = false) attachment: MultipartFile?
    ): ResponseEntity<InvoiceResponse> {
        logger.info("Updating invoice with ID: {}", id)

        // Convert string to UpdateInvoiceRequest
        val updateRequest = invoiceObjectMapper.readValue(invoice, UpdateInvoiceRequest::class.java)

        val invoice = invoiceService.updateInvoice(id, updateRequest, attachment)
        val response = invoice.toResponse()

        return ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an invoice", description = "Deletes an invoice by its ID")
    fun deleteInvoice(
        @Parameter(description = "Invoice ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting invoice with ID: {}", id)

        val deleted = invoiceService.deleteInvoice(id)

        return if (deleted) {
            ok(createSuccessResponse("Invoice successfully deleted", mapOf("invoiceId" to id)))
        } else {
            badRequest("Failed to delete invoice")
        }
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update invoice status", description = "Updates the status of an invoice")
    fun updateInvoiceStatus(
        @Parameter(description = "Invoice ID", required = true) @PathVariable id: String,
        @Parameter(description = "New status", required = true) @RequestParam status: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Updating status of invoice with ID: {} to: {}", id, status)

        val updated = invoiceService.updateInvoiceStatus(id, status)

        return if (updated) {
            ok(createSuccessResponse("Invoice status successfully updated",
                mapOf("invoiceId" to id, "status" to status)))
        } else {
            badRequest("Failed to update invoice status")
        }
    }

    @GetMapping("/{id}/attachment")
    @Operation(summary = "Get invoice attachment", description = "Downloads the attachment of an invoice")
    fun getInvoiceAttachment(
        @Parameter(description = "Invoice ID", required = true) @PathVariable id: String
    ): ResponseEntity<Resource> {
        logger.info("Getting attachment for invoice with ID: {}", id)

        val attachmentData = invoiceService.getInvoiceAttachment(id)
            ?: return ResponseEntity.notFound().build()

        val (fileBytes, contentType) = attachmentData
        val resource = ByteArrayResource(fileBytes)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-$id\"")
            .body(resource)
    }

    @PostMapping("/extract")
    @Operation(summary = "Extract invoice data", description = "Extracts data from an invoice file")
    fun extractInvoiceData(
        @Parameter(description = "Invoice file", required = true) @RequestPart("file") file: MultipartFile
    ): ResponseEntity<InvoiceDataResponse> {
        logger.info("Extracting data from invoice file: {}", file.originalFilename)

        val extractedData = invoiceService.extractInvoiceData(file)
        val response = InvoiceDataResponse(extractedInvoiceData = extractedData)

        return ok(response)
    }

    // Funkcja pomocnicza do konwersji modelu domenowego na DTO odpowiedzi
    private fun Invoice.toResponse(): InvoiceResponse {
        return InvoiceResponse(
            id = id.value,
            number = number,
            title = title,
            issuedDate = issuedDate,
            dueDate = dueDate,
            sellerName = sellerName,
            sellerTaxId = sellerTaxId,
            sellerAddress = sellerAddress,
            buyerName = buyerName,
            buyerTaxId = buyerTaxId,
            buyerAddress = buyerAddress,
            clientId = clientId?.value,
            status = status.name,
            type = type.name,
            paymentMethod = paymentMethod.name,
            totalNet = totalNet,
            totalTax = totalTax,
            totalGross = totalGross,
            currency = currency,
            notes = notes,
            protocolId = protocolId,
            protocolNumber = protocolNumber,
            createdAt = audit.createdAt,
            updatedAt = audit.updatedAt,
            items = items.map { item ->
                InvoiceItemDTO(
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
            attachment = attachment?.let { att ->
                InvoiceAttachmentDTO(
                    id = att.id,
                    name = att.name,
                    size = att.size,
                    type = att.type,
                    uploadedAt = att.uploadedAt
                )
            }
        )
    }
}