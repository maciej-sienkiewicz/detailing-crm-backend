package com.carslab.crm.finances.domain

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.api.model.BuyerInfoDTO
import com.carslab.crm.api.model.DocumentType
import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.ExtractedDocumentDataDTO
import com.carslab.crm.api.model.ExtractedItemDTO
import com.carslab.crm.api.model.FinancialSummaryResponse
import com.carslab.crm.api.model.GeneralInfoDTO
import com.carslab.crm.api.model.SellerInfoDTO
import com.carslab.crm.api.model.SummaryDTO
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.api.model.UnifiedDocumentFilterDTO
import com.carslab.crm.api.model.request.CreateUnifiedDocumentRequest
import com.carslab.crm.api.model.request.UpdateUnifiedDocumentRequest
import com.carslab.crm.domain.model.ApprovalStatus
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.infrastructure.events.InvoiceCreatedEvent
import com.carslab.crm.modules.finances.domain.balance.DocumentBalanceService
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.modules.finances.api.requests.InvoiceGenerationFromVisitRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceGenerationResponse
import com.carslab.crm.modules.finances.domain.InvoiceAttachmentGenerationService
import com.carslab.crm.modules.finances.domain.balance.BalanceService
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.infrastructure.mapper.EnumMappers
import com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UnifiedDocumentService(
    private val documentRepository: UnifiedDocumentRepository,
    private val documentStorageService: UniversalStorageService,
    private val securityContext: SecurityContext,
    private val documentBalanceService: DocumentBalanceService,
    private val balanceService: BalanceService,
    private val eventsPublisher: EventPublisher,
    private val visitDetailQueryService: VisitDetailQueryService,
    private val invoiceAttachmentGenerationService: InvoiceAttachmentGenerationService,
    private val companySettingsApplicationService: CompanyDetailsFetchService,
) {
    private val logger = LoggerFactory.getLogger(UnifiedDocumentService::class.java)

    @Transactional
    fun createDocument(request: CreateUnifiedDocumentRequest, attachmentFile: MultipartFile?): UnifiedFinancialDocument {
        logger.info("Creating new financial document: {}", request.title)
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        validateDocumentRequest(request)
        val document = convertToDocumentModel(request)

        val attachment = attachmentFile?.let {
            val storageId = documentStorageService.storeFile(
                UniversalStoreRequest(
                    file = it,
                    originalFileName = it.originalFilename ?: "document.pdf",
                    contentType = it.contentType ?: "application/pdf",
                    companyId = companyId,
                    entityId = document.id.value,
                    entityType = "document",
                    category = "finances",
                    subCategory = when (document.type) {
                        DocumentType.INVOICE -> "invoices/${document.direction.name.lowercase()}"
                        DocumentType.RECEIPT -> "receipts"
                        else -> "documents"
                    },
                    description = "Financial document attachment",
                    date = document.issuedDate,
                    tags = mapOf(
                        "documentType" to document.type.name,
                        "direction" to document.direction.name
                    )
                )
            )

            DocumentAttachment(
                id = UUID.randomUUID().toString(),
                name = it.originalFilename ?: "document.pdf",
                size = it.size,
                type = it.contentType ?: "application/octet-stream",
                storageId = storageId,
                uploadedAt = LocalDateTime.now()
            )
        }

        val completeDocument = if (attachment != null) {
            document.copy(attachment = attachment)
        } else {
            document
        }

        val savedDocument = documentRepository.save(completeDocument)
        logger.info("Created document with ID: {}", savedDocument.id.value)

        try {
            documentBalanceService.handleDocumentChange(
                document = savedDocument,
                oldStatus = null,
                companyId = companyId
            )
        } catch (e: Exception) {
            logger.warn("Failed to update balance for document ${savedDocument.id.value}: ${e.message}")
        }

        val visitDetails = document.protocolId?.let {
            visitDetailQueryService.getSimpleDetails(it)
        }

        eventsPublisher.publish(
            InvoiceCreatedEvent.create(
                visit = visitDetails,
                document = savedDocument,
                userId = securityContext.getCurrentUserId(),
                companyId = companyId,
                userName = securityContext.getCurrentUserName(),
            )
        )

        return savedDocument
    }

    @Transactional
    fun updateDocument(id: String, request: UpdateUnifiedDocumentRequest, attachmentFile: MultipartFile?): UnifiedFinancialDocument {
        logger.info("Updating document with ID: {}", id)
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val existingDocument = documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)

        val oldStatus = existingDocument.status

        validateDocumentRequest(request)

        val updatedDocument = convertToDocumentModel(request, existingDocument)

        val attachment = when {
            attachmentFile != null -> {
                existingDocument.attachment?.let {
                    documentStorageService.deleteFile(it.storageId)
                }

                val storageId = documentStorageService.storeFile(
                    UniversalStoreRequest(
                        file = attachmentFile,
                        originalFileName = attachmentFile.originalFilename ?: "document.pdf",
                        contentType = attachmentFile.contentType ?: "application/pdf",
                        companyId = companyId,
                        entityId = existingDocument.id.value,
                        entityType = "document",
                        category = "finances",
                        subCategory = when (updatedDocument.type) {
                            DocumentType.INVOICE -> "invoices/${updatedDocument.direction.name.lowercase()}"
                            DocumentType.RECEIPT -> "receipts"
                            else -> "documents"
                        },
                        description = "Financial document attachment",
                        date = updatedDocument.issuedDate,
                        tags = mapOf(
                            "documentType" to updatedDocument.type.name,
                            "direction" to updatedDocument.direction.name
                        )
                    )
                )

                DocumentAttachment(
                    id = UUID.randomUUID().toString(),
                    name = attachmentFile.originalFilename ?: "document.pdf",
                    size = attachmentFile.size,
                    type = attachmentFile.contentType ?: "application/octet-stream",
                    storageId = storageId,
                    uploadedAt = LocalDateTime.now()
                )
            }
            else -> existingDocument.attachment
        }

        val completeDocument = updatedDocument.copy(attachment = attachment)

        val savedDocument = documentRepository.save(completeDocument)

        try {
            documentBalanceService.handleDocumentChange(
                document = savedDocument,
                oldStatus = oldStatus,
                companyId = companyId
            )
        } catch (e: Exception) {
            logger.warn("Failed to update balance for document ${savedDocument.id.value}: ${e.message}")
        }

        logger.info("Updated document with ID: {}", savedDocument.id.value)

        return savedDocument
    }

    @Transactional
    fun updateDocumentWithAttachment(document: UnifiedFinancialDocument): UnifiedFinancialDocument {
        logger.info("Updating document with new attachment: {}", document.id.value)

        val savedDocument = documentRepository.save(document)

        logger.info("Document attachment updated successfully: {}", savedDocument.id.value)

        return savedDocument
    }

    fun getDocumentById(id: String): UnifiedFinancialDocument {
        logger.debug("Getting document by ID: {}", id)
        return documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)
    }

    @Transactional(readOnly = true)
    fun getAllDocuments(filter: UnifiedDocumentFilterDTO? = null, page: Int = 0, size: Int = 10): PaginatedResult<UnifiedFinancialDocument> {
        logger.debug("Getting all documents with filter: {}, page: {}, size: {}", filter, page, size)
        return documentRepository.findAll(filter, page, size)
    }

    @Transactional
    fun deleteDocument(id: String): Boolean {
        logger.info("Deleting document with ID: {}", id)
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val document = documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)

        try {
            documentBalanceService.handleDocumentDeletion(document, companyId)
        } catch (e: Exception) {
            logger.warn("Failed to reverse balance for deleted document $id: ${e.message}")
        }

        document.attachment?.let {
            try {
                documentStorageService.deleteFile(it.storageId)
            } catch (e: Exception) {
                logger.warn("Failed to delete attachment for document $id: ${e.message}")
            }
        }

        return documentRepository.deleteById(UnifiedDocumentId(id))
    }

    @Transactional
    fun updateDocumentStatus(id: String, status: String): Boolean {
        logger.info("Updating status of document with ID: {} to: {}", id, status)
        val companyId = securityContext.getCurrentCompanyId()

        val document = documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)

        val oldStatus = document.status

        val updated = documentRepository.updateStatus(UnifiedDocumentId(id), status)

        if (updated) {
            try {
                val updatedDocument = document.copy(status = DocumentStatus.valueOf(status))
                documentBalanceService.handleDocumentChange(
                    document = updatedDocument,
                    oldStatus = oldStatus,
                    companyId = companyId
                )
            } catch (e: Exception) {
                logger.warn("Failed to update balance for status change of document $id: ${e.message}")
            }
        }

        return updated
    }

    @Transactional
    fun updatePaidAmount(id: String, paidAmount: BigDecimal): Boolean {
        logger.info("Updating paid amount of document with ID: {} to: {}", id, paidAmount)
        val companyId = securityContext.getCurrentCompanyId()

        val document = documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)

        val oldStatus = document.status

        val newStatus = when {
            paidAmount >= document.totalGross -> DocumentStatus.PAID.name
            paidAmount > BigDecimal.ZERO -> DocumentStatus.PARTIALLY_PAID.name
            else -> document.status.name
        }

        val updated = documentRepository.updatePaidAmount(UnifiedDocumentId(id), paidAmount, newStatus)

        if (updated) {
            try {
                val updatedDocument = document.copy(
                    paidAmount = paidAmount,
                    status = DocumentStatus.valueOf(newStatus)
                )
                documentBalanceService.handleDocumentChange(
                    document = updatedDocument,
                    oldStatus = oldStatus,
                    companyId = companyId
                )
            } catch (e: Exception) {
                logger.warn("Failed to update balance for paid amount change of document $id: ${e.message}")
            }
        }

        return updated
    }

    fun getDocumentAttachment(id: String): Pair<ByteArray, String>? {
        logger.debug("Getting attachment for document with ID: {}", id)

        val document = documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)

        return document.attachment?.let {
            try {
                val fileBytes = documentStorageService.retrieveFile(it.storageId)
                fileBytes?.let { bytes -> bytes to it.type }
            } catch (e: Exception) {
                logger.error("Failed to retrieve attachment for document $id: ${e.message}")
                null
            }
        }
    }
    
    private fun generateAndStorePermanentInvoicePdf(document: UnifiedFinancialDocument, companyId: Long): UnifiedFinancialDocument {
        return try {
            // Generuj tymczasowy PDF
            val tempAttachment = invoiceAttachmentGenerationService.generateInvoiceAttachmentWithoutSignature(document)
                ?: return document

            // Pobierz bytes z tymczasowego pliku
            val pdfBytes = documentStorageService.retrieveFile(tempAttachment.storageId)
                ?: return document

            // Usuń tymczasowy plik
            try {
                documentStorageService.deleteFile(tempAttachment.storageId)
            } catch (e: Exception) {
                logger.warn("Failed to cleanup temporary file: ${tempAttachment.storageId}", e)
            }

            // Zapisz w właściwym katalogu
            val permanentStorageId = documentStorageService.storeFile(
                UniversalStoreRequest(
                    file = createMultipartFile(pdfBytes, document),
                    originalFileName = "invoice-${document.number}.pdf",
                    contentType = "application/pdf",
                    companyId = companyId,
                    entityId = document.id.value,
                    entityType = "document",
                    category = "finances",
                    subCategory = "invoices/${document.direction.name.lowercase()}",
                    description = "Generated invoice PDF without signature",
                    date = document.issuedDate,
                    tags = mapOf(
                        "documentType" to document.type.name,
                        "direction" to document.direction.name,
                        "signed" to "false",
                        "version" to "unsigned",
                        "originalNumber" to document.number,
                        "generated" to "true"
                    )
                )
            )

            // Utwórz stały załącznik
            val permanentAttachment = DocumentAttachment(
                id = UUID.randomUUID().toString(),
                name = "invoice-${document.number}.pdf",
                size = pdfBytes.size.toLong(),
                type = "application/pdf",
                storageId = permanentStorageId,
                uploadedAt = LocalDateTime.now()
            )

            // Zaktualizuj dokument z załącznikiem
            val updatedDocument = document.copy(attachment = permanentAttachment)
            updateDocumentWithAttachment(updatedDocument)

        } catch (e: Exception) {
            logger.error("Failed to generate permanent invoice PDF for document: ${document.id.value}", e)
            document
        }
    }

    private fun createMultipartFile(pdfBytes: ByteArray, document: UnifiedFinancialDocument): MultipartFile {
        return object : MultipartFile {
            override fun getName(): String = "invoice"
            override fun getOriginalFilename(): String = "invoice-${document.number}.pdf"
            override fun getContentType(): String = "application/pdf"
            override fun isEmpty(): Boolean = pdfBytes.isEmpty()
            override fun getSize(): Long = pdfBytes.size.toLong()
            override fun getBytes(): ByteArray = pdfBytes
            override fun getInputStream(): java.io.InputStream = pdfBytes.inputStream()
            override fun transferTo(dest: java.io.File): Unit = throw UnsupportedOperationException("Transfer not supported")
        }
    }

    private fun createDocumentInternal(document: UnifiedFinancialDocument, attachmentFile: MultipartFile?): UnifiedFinancialDocument {
        val companyId = securityContext.getCurrentCompanyId()

        validateDocumentRequest(document)

        val attachment = attachmentFile?.let {
            val storageId = documentStorageService.storeFile(
                UniversalStoreRequest(
                    file = it,
                    originalFileName = it.originalFilename ?: "document.pdf",
                    contentType = it.contentType ?: "application/pdf",
                    companyId = companyId,
                    entityId = document.id.value,
                    entityType = "document",
                    category = "finances",
                    subCategory = when (document.type) {
                        DocumentType.INVOICE -> "invoices/${document.direction.name.lowercase()}"
                        DocumentType.RECEIPT -> "receipts"
                        else -> "documents"
                    },
                    description = "Financial document attachment",
                    date = document.issuedDate,
                    tags = mapOf(
                        "documentType" to document.type.name,
                        "direction" to document.direction.name
                    )
                )
            )

            DocumentAttachment(
                id = UUID.randomUUID().toString(),
                name = it.originalFilename ?: "document.pdf",
                size = it.size,
                type = it.contentType ?: "application/octet-stream",
                storageId = storageId,
                uploadedAt = LocalDateTime.now()
            )
        }

        val completeDocument = if (attachment != null) {
            document.copy(attachment = attachment)
        } else {
            document
        }

        val savedDocument = documentRepository.save(completeDocument)
        logger.info("Created document with ID: {}", savedDocument.id.value)

        try {
            documentBalanceService.handleDocumentChange(
                document = savedDocument,
                oldStatus = null,
                companyId = companyId
            )
        } catch (e: Exception) {
            logger.warn("Failed to update balance for document ${savedDocument.id.value}: ${e.message}")
        }

        val visitDetails = document.protocolId?.let {
            visitDetailQueryService.getSimpleDetails(it)
        }

        eventsPublisher.publish(
            InvoiceCreatedEvent.create(
                visit = visitDetails,
                document = savedDocument,
                userId = securityContext.getCurrentUserId(),
                companyId = companyId,
                userName = securityContext.getCurrentUserName(),
            )
        )

        return savedDocument
    }

    private fun validateDocumentRequest(document: UnifiedFinancialDocument) {
        document.dueDate?.let { dueDate ->
            if (dueDate.isBefore(document.issuedDate)) {
                throw ValidationException("Due date cannot be before issued date")
            }
        }

        if (document.items.isNotEmpty()) {
            val calculatedTotalNet = document.items.sumOf { it.totalNet }
            val calculatedTotalGross = document.items.sumOf { it.totalGross }

            // Użyj bezpiecznego porównywania z tolerancją
            if (!CalculationUtils.safeCompare(calculatedTotalNet, document.totalNet)) {
                logger.warn("Total net mismatch: calculated={}, expected={}", calculatedTotalNet, document.totalNet)
                throw ValidationException("Total net amount does not match sum of items: calculated=$calculatedTotalNet, expected=${document.totalNet}")
            }

            if (!CalculationUtils.safeCompare(calculatedTotalGross, document.totalGross)) {
                logger.warn("Total gross mismatch: calculated={}, expected={}", calculatedTotalGross, document.totalGross)
                throw ValidationException("Total gross amount does not match sum of items: calculated=$calculatedTotalGross, expected=${document.totalGross}")
            }
        }
    }

    fun extractDocumentData(file: MultipartFile): ExtractedDocumentDataDTO {
        logger.info("Extracting data from document file: {}", file.originalFilename)

        return ExtractedDocumentDataDTO(
            generalInfo = GeneralInfoDTO(
                title = "Dokument transakcyjny",
                issuedDate = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(14)
            ),
            seller = SellerInfoDTO(
                name = "Detailing Pro Sp. z o.o.",
                taxId = "1234567890",
                address = "ul. Polerska 15, 00-123 Warszawa"
            ),
            buyer = BuyerInfoDTO(
                name = "Przykładowy Klient",
                taxId = "9876543210",
                address = "ul. Testowa 10, 00-001 Warszawa"
            ),
            items = listOf(
                ExtractedItemDTO(
                    name = "Usługa detailingowa Premium",
                    description = "Kompleksowe czyszczenie pojazdu",
                    quantity = BigDecimal("1"),
                    unitPrice = BigDecimal("1000"),
                    taxRate = BigDecimal("23"),
                    totalNet = BigDecimal("1000"),
                    totalGross = BigDecimal("1230")
                )
            ),
            summary = SummaryDTO(
                totalNet = BigDecimal("1000"),
                totalTax = BigDecimal("230"),
                totalGross = BigDecimal("1230")
            ),
            notes = "Dziękujemy za skorzystanie z naszych usług."
        )
    }

    @Transactional(readOnly = true)
    fun getFinancialSummary(dateFrom: LocalDate?, dateTo: LocalDate?): FinancialSummaryResponse {
        logger.info("Getting financial summary for period: {} to {}", dateFrom, dateTo)
        val companyId = securityContext.getCurrentCompanyId()
        val balance = balanceService.getCurrentBalances(companyId)
        return documentRepository.getFinancialSummary(dateFrom, dateTo)
            .copy(
                cashBalance = balance.cashBalance,
                bankAccountBalance = balance.bankBalance
            )
    }

    @Transactional(readOnly = true)
    fun getChartData(period: String): Map<String, Any> {
        logger.info("Getting chart data for period: {}", period)
        return documentRepository.getChartData(period)
    }

    @Transactional
    fun markOverdueDocuments() {
        logger.info("Marking overdue documents")
        val companyId = securityContext.getCurrentCompanyId()

        val today = LocalDate.now()
        val overdueDocuments = documentRepository.findOverdueBefore(today)

        var marked = 0
        for (document in overdueDocuments) {
            if (document.status == DocumentStatus.NOT_PAID || document.status == DocumentStatus.PARTIALLY_PAID) {
                try {
                    val oldStatus = document.status
                    val updated = documentRepository.updateStatus(document.id, DocumentStatus.OVERDUE.name)

                    if (updated) {
                        try {
                            val overdueDocument = document.copy(status = DocumentStatus.OVERDUE)
                            documentBalanceService.handleDocumentChange(
                                document = overdueDocument,
                                oldStatus = oldStatus,
                                companyId = companyId
                            )
                        } catch (e: Exception) {
                            logger.warn("Failed to update balance for overdue document ${document.id.value}: ${e.message}")
                        }
                        marked++
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to mark document ${document.id.value} as overdue: ${e.message}")
                }
            }
        }

        logger.info("Marked {} documents as overdue", marked)
    }

    private fun convertToDocumentModel(request: CreateUnifiedDocumentRequest): UnifiedFinancialDocument {
        val now = LocalDateTime.now()
        return UnifiedFinancialDocument(
            id = UnifiedDocumentId.generate(),
            number = "",
            type = DocumentType.valueOf(request.type.uppercase()),
            title = request.title,
            description = request.description,
            issuedDate = request.issuedDate,
            dueDate = request.dueDate,
            sellerName = request.sellerName,
            sellerTaxId = request.sellerTaxId,
            sellerAddress = request.sellerAddress,
            buyerName = request.buyerName,
            buyerTaxId = request.buyerTaxId,
            buyerAddress = request.buyerAddress,
            status = if (request.dueDate != null && LocalDate.now().isAfter(request.dueDate))
                DocumentStatus.OVERDUE else DocumentStatus.valueOf(request.status.uppercase()),
            direction = TransactionDirection.valueOf(request.direction.uppercase()),
            paymentMethod = PaymentMethod.valueOf(request.paymentMethod.uppercase()),
            totalNet = request.totalNet,
            totalTax = request.totalTax,
            totalGross = request.totalGross,
            paidAmount = request.paidAmount ?: BigDecimal.ZERO,
            currency = request.currency,
            notes = request.notes,
            protocolId = request.protocolId,
            protocolNumber = request.protocolNumber,
            visitId = request.visitId,
            items = request.items.map { item ->
                DocumentItem(
                    id = item.id ?: UUID.randomUUID().toString(),
                    name = item.name,
                    description = item.description,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    taxRate = item.taxRate,
                    totalNet = item.totalNet,
                    totalGross = item.totalGross
                )
            },
            attachment = null,
            audit = Audit(
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun convertToDocumentModel(request: UpdateUnifiedDocumentRequest, existingDocument: UnifiedFinancialDocument): UnifiedFinancialDocument {
        return UnifiedFinancialDocument(
            id = existingDocument.id,
            number = existingDocument.number,
            type = DocumentType.valueOf(request.type.uppercase()),
            title = request.title,
            description = request.description,
            issuedDate = request.issuedDate,
            dueDate = request.dueDate,
            sellerName = request.sellerName,
            sellerTaxId = request.sellerTaxId,
            sellerAddress = request.sellerAddress,
            buyerName = request.buyerName,
            buyerTaxId = request.buyerTaxId,
            buyerAddress = request.buyerAddress,
            status = DocumentStatus.valueOf(request.status.uppercase()),
            direction = TransactionDirection.valueOf(request.direction.uppercase()),
            paymentMethod = PaymentMethod.valueOf(request.paymentMethod.uppercase()),
            totalNet = request.totalNet,
            totalTax = request.totalTax,
            totalGross = request.totalGross,
            paidAmount = request.paidAmount ?: BigDecimal.ZERO,
            currency = request.currency,
            notes = request.notes,
            protocolId = request.protocolId,
            protocolNumber = request.protocolNumber,
            visitId = request.visitId,
            items = request.items.map { item ->
                DocumentItem(
                    id = item.id ?: UUID.randomUUID().toString(),
                    name = item.name,
                    description = item.description,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    taxRate = item.taxRate,
                    totalNet = item.totalNet,
                    totalGross = item.totalGross
                )
            },
            attachment = existingDocument.attachment,
            audit = Audit(
                createdAt = existingDocument.audit.createdAt,
                updatedAt = LocalDateTime.now()
            )
        )
    }

    private fun validateDocumentRequest(request: Any) {
        when (request) {
            is CreateUnifiedDocumentRequest -> {
                request.dueDate?.let { dueDate ->
                    if (dueDate.isBefore(request.issuedDate)) {
                        throw ValidationException("Due date cannot be before issued date")
                    }
                }

                if (request.items.isNotEmpty()) {
                    val calculatedTotalNet = request.items.sumOf { it.totalNet }
                    val calculatedTotalGross = request.items.sumOf { it.totalGross }

                    // Użyj bezpiecznego porównywania z tolerancją
                    if (!CalculationUtils.safeCompare(calculatedTotalNet, request.totalNet)) {
                        logger.warn(
                            "Total net mismatch: calculated={}, expected={}",
                            calculatedTotalNet,
                            request.totalNet
                        )
                        throw ValidationException("Total net amount does not match sum of items: calculated=$calculatedTotalNet, expected=${request.totalNet}")
                    }

                    if (!CalculationUtils.safeCompare(calculatedTotalGross, request.totalGross)) {
                        logger.warn(
                            "Total gross mismatch: calculated={}, expected={}",
                            calculatedTotalGross,
                            request.totalGross
                        )
                        throw ValidationException("Total gross amount does not match sum of items: calculated=$calculatedTotalGross, expected=${request.totalGross}")
                    }
                }
            }

            is UpdateUnifiedDocumentRequest -> {
                request.dueDate?.let { dueDate ->
                    if (dueDate.isBefore(request.issuedDate)) {
                        throw ValidationException("Due date cannot be before issued date")
                    }
                }

                if (request.items.isNotEmpty()) {
                    val calculatedTotalNet = request.items.sumOf { it.totalNet }
                    val calculatedTotalGross = request.items.sumOf { it.totalGross }

                    // Użyj bezpiecznego porównywania z tolerancją
                    if (!CalculationUtils.safeCompare(calculatedTotalNet, request.totalNet)) {
                        logger.warn(
                            "Total net mismatch: calculated={}, expected={}",
                            calculatedTotalNet,
                            request.totalNet
                        )
                        throw ValidationException("Total net amount does not match sum of items: calculated=$calculatedTotalNet, expected=${request.totalNet}")
                    }

                    if (!CalculationUtils.safeCompare(calculatedTotalGross, request.totalGross)) {
                        logger.warn(
                            "Total gross mismatch: calculated={}, expected={}",
                            calculatedTotalGross,
                            request.totalGross
                        )
                        throw ValidationException("Total gross amount does not match sum of items: calculated=$calculatedTotalGross, expected=${request.totalGross}")
                    }
                }
            }
        }
    }
}

data class PaginatedResult<T>(
    val data: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int
)