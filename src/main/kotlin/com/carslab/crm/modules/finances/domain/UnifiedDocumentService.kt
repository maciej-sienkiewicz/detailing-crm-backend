package com.carslab.crm.finances.domain

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
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.modules.finances.domain.balance.DocumentBalanceService
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.security.SecurityContext
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
    private val documentStorageService: UnifiedDocumentStorageService,
    private val securityContext: SecurityContext,
    private val documentBalanceService: DocumentBalanceService
) {
    private val logger = LoggerFactory.getLogger(UnifiedDocumentService::class.java)

    @Transactional
    fun createDocument(request: CreateUnifiedDocumentRequest, attachmentFile: MultipartFile?): UnifiedFinancialDocument {
        logger.info("Creating new financial document: {}", request.title)
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        validateDocumentRequest(request)

        val document = convertToDocumentModel(request)

        val attachment = attachmentFile?.let {
            val attachmentId = documentStorageService.storeDocumentFile(it, document.id)
            DocumentAttachment(
                name = it.originalFilename ?: "document.pdf",
                size = it.size,
                type = it.contentType ?: "application/octet-stream",
                storageId = attachmentId,
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
                    documentStorageService.deleteDocumentFile(it.storageId)
                }

                val attachmentId = documentStorageService.storeDocumentFile(attachmentFile, existingDocument.id)
                DocumentAttachment(
                    name = attachmentFile.originalFilename ?: "document.pdf",
                    size = attachmentFile.size,
                    type = attachmentFile.contentType ?: "application/octet-stream",
                    storageId = attachmentId,
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
                documentStorageService.deleteDocumentFile(it.storageId)
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
                val fileBytes = documentStorageService.getDocumentFile(it.storageId)
                fileBytes to it.type
            } catch (e: Exception) {
                logger.error("Failed to retrieve attachment for document $id: ${e.message}")
                null
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
        return documentRepository.getFinancialSummary(dateFrom, dateTo)
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
            type = DocumentType.valueOf(request.type),
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
                DocumentStatus.OVERDUE else DocumentStatus.valueOf(request.status),
            direction = TransactionDirection.valueOf(request.direction),
            paymentMethod = PaymentMethod.valueOf(request.paymentMethod),
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
            type = DocumentType.valueOf(request.type),
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
            status = DocumentStatus.valueOf(request.status),
            direction = TransactionDirection.valueOf(request.direction),
            paymentMethod = PaymentMethod.valueOf(request.paymentMethod),
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

                    if (calculatedTotalNet.compareTo(request.totalNet) != 0) {
                        throw ValidationException("Total net amount does not match sum of items")
                    }

                    if (calculatedTotalGross.compareTo(request.totalGross) != 0) {
                        throw ValidationException("Total gross amount does not match sum of items")
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

                    if (calculatedTotalNet.compareTo(request.totalNet) != 0) {
                        throw ValidationException("Total net amount does not match sum of items")
                    }

                    if (calculatedTotalGross.compareTo(request.totalGross) != 0) {
                        throw ValidationException("Total gross amount does not match sum of items")
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