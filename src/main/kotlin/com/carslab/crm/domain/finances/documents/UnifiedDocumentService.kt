package com.carslab.crm.domain.finances.documents

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
import com.carslab.crm.domain.finances.documents.UnifiedDocumentStorageService
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.domain.port.UnifiedDocumentRepository
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.persistence.entity.CashBalanceEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.BankAccountBalanceRepository
import com.carslab.crm.infrastructure.persistence.repository.CashBalancesRepository
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.print.Doc

@Service
class UnifiedDocumentService(
    private val documentRepository: UnifiedDocumentRepository,
    private val documentStorageService: UnifiedDocumentStorageService,
    private val cashBalancesRepository: CashBalancesRepository,
    private val bankAccountBalanceRepository: BankAccountBalanceRepository
) {
    private val logger = LoggerFactory.getLogger(UnifiedDocumentService::class.java)

    /**
     * Tworzy nowy dokument finansowy.
     */
    @Transactional
    fun createDocument(request: CreateUnifiedDocumentRequest, attachmentFile: MultipartFile?): UnifiedFinancialDocument {
        logger.info("Creating new financial document: {}", request.title)
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // Walidacja
        validateDocumentRequest(request)

        // Tworzenie dokumentu z obiektu DTO
        val document = convertToDocumentModel(request)

        // Obsługa załącznika, jeśli istnieje
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

        // Uzupełnienie dokumentu o załącznik
        val completeDocument = if (attachment != null) {
            document.copy(attachment = attachment)
        } else {
            document
        }

        // Zapisanie dokumentu w repozytorium
        val savedDocument = documentRepository.save(completeDocument)
        logger.info("Created document with ID: {}", savedDocument.id.value)

        updateBalanceForDocument(savedDocument, companyId)

        return savedDocument
    }

    private fun updateBalanceForDocument(
        document: UnifiedFinancialDocument,
        companyId: Long
    ) {
        // Aktualizuj saldo tylko dla opłaconych dokumentów
        if (document.status != DocumentStatus.PAID) return

        val amount = document.totalGross
        val timestamp = Instant.now().toString()
        val isIncome = document.direction == TransactionDirection.INCOME

        when (document.paymentMethod) {
            PaymentMethod.CASH -> {
                if (isIncome) {
                    cashBalancesRepository.addAmountToBalance(companyId, amount, timestamp)
                } else {
                    cashBalancesRepository.subtractAmountFromBalance(companyId, amount, timestamp)
                }
            }
            PaymentMethod.CARD, PaymentMethod.BANK_TRANSFER -> {
                if (isIncome) {
                    bankAccountBalanceRepository.addAmountToBalance(companyId, amount, timestamp)
                } else {
                    bankAccountBalanceRepository.subtractAmountFromBalance(companyId, amount, timestamp)
                }
            }
            else -> {
                // PaymentMethod.MOBILE_PAYMENT, OTHER - można dodać obsługę w przyszłości
                logger.debug("Balance update not implemented for payment method: ${document.paymentMethod}")
            }
        }
    }

    /**
     * Aktualizuje istniejący dokument.
     */
    @Transactional
    fun updateDocument(id: String, request: UpdateUnifiedDocumentRequest, attachmentFile: MultipartFile?): UnifiedFinancialDocument {
        logger.info("Updating document with ID: {}", id)

        // Sprawdzenie czy dokument istnieje
        val existingDocument = documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)

        // Walidacja
        validateDocumentRequest(request)

        // Konwersja DTO na model domenowy
        val updatedDocument = convertToDocumentModel(request, existingDocument)

        // Obsługa załącznika
        val attachment = when {
            // Nowy załącznik
            attachmentFile != null -> {
                // Jeśli istniał poprzedni załącznik, usuń go
                existingDocument.attachment?.let {
                    documentStorageService.deleteDocumentFile(it.storageId)
                }

                // Zapisz nowy załącznik
                val attachmentId = documentStorageService.storeDocumentFile(attachmentFile, existingDocument.id)
                DocumentAttachment(
                    name = attachmentFile.originalFilename ?: "document.pdf",
                    size = attachmentFile.size,
                    type = attachmentFile.contentType ?: "application/octet-stream",
                    storageId = attachmentId,
                    uploadedAt = LocalDateTime.now()
                )
            }
            // Zachowanie istniejącego załącznika
            else -> existingDocument.attachment
        }

        // Uzupełnienie dokumentu o załącznik
        val completeDocument = updatedDocument.copy(attachment = attachment)

        // Zapisanie zaktualizowanego dokumentu
        val savedDocument = documentRepository.save(completeDocument)
        logger.info("Updated document with ID: {}", savedDocument.id.value)

        return savedDocument
    }

    /**
     * Pobiera dokument po ID.
     */
    fun getDocumentById(id: String): UnifiedFinancialDocument {
        logger.debug("Getting document by ID: {}", id)
        return documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)
    }

    /**
     * Pobiera wszystkie dokumenty z opcjonalnym filtrowaniem i paginacją.
     */
    fun getAllDocuments(filter: UnifiedDocumentFilterDTO? = null, page: Int = 0, size: Int = 10): PaginatedResult<UnifiedFinancialDocument> {
        logger.debug("Getting all documents with filter: {}, page: {}, size: {}", filter, page, size)
        return documentRepository.findAll(filter, page, size)
    }

    /**
     * Usuwa dokument po ID.
     */
    @Transactional
    fun deleteDocument(id: String): Boolean {
        logger.info("Deleting document with ID: {}", id)

        // Sprawdzenie czy dokument istnieje
        val document = documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)

        // Usunięcie załącznika, jeśli istnieje
        document.attachment?.let {
            documentStorageService.deleteDocumentFile(it.storageId)
        }

        // Usunięcie dokumentu z repozytorium
        return documentRepository.deleteById(UnifiedDocumentId(id))
    }

    /**
     * Aktualizuje status dokumentu.
     */
    @Transactional
    fun updateDocumentStatus(id: String, status: String): Boolean {
        logger.info("Updating status of document with ID: {} to: {}", id, status)

        // Sprawdzenie czy dokument istnieje
        if (documentRepository.findById(UnifiedDocumentId(id)) == null) {
            throw ResourceNotFoundException("Document", id)
        }

        return documentRepository.updateStatus(UnifiedDocumentId(id), status)
    }

    /**
     * Aktualizuje kwotę zapłaconą.
     */
    @Transactional
    fun updatePaidAmount(id: String, paidAmount: BigDecimal): Boolean {
        logger.info("Updating paid amount of document with ID: {} to: {}", id, paidAmount)

        // Sprawdzenie czy dokument istnieje
        val document = documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)

        // Automatyczna aktualizacja statusu na podstawie kwoty zapłaconej
        val newStatus = when {
            paidAmount >= document.totalGross -> DocumentStatus.PAID.name
            paidAmount > BigDecimal.ZERO -> DocumentStatus.PARTIALLY_PAID.name
            else -> document.status.name
        }

        return documentRepository.updatePaidAmount(UnifiedDocumentId(id), paidAmount, newStatus)
    }

    /**
     * Pobiera załącznik dokumentu.
     */
    fun getDocumentAttachment(id: String): Pair<ByteArray, String>? {
        logger.debug("Getting attachment for document with ID: {}", id)

        // Sprawdzenie czy dokument istnieje
        val document = documentRepository.findById(UnifiedDocumentId(id))
            ?: throw ResourceNotFoundException("Document", id)

        // Jeśli dokument ma załącznik, pobierz go
        return document.attachment?.let {
            val fileBytes = documentStorageService.getDocumentFile(it.storageId)
            fileBytes to it.type
        }
    }

    /**
     * Ekstrakcja danych z pliku dokumentu.
     */
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

    /**
     * Pobiera podsumowanie finansowe.
     */
    fun getFinancialSummary(dateFrom: LocalDate?, dateTo: LocalDate?): FinancialSummaryResponse {
        logger.info("Getting financial summary for period: {} to {}", dateFrom, dateTo)
        return documentRepository.getFinancialSummary(dateFrom, dateTo)
    }

    /**
     * Pobiera dane do wykresów.
     */
    fun getChartData(period: String): Map<String, Any> {
        logger.info("Getting chart data for period: {}", period)
        return documentRepository.getChartData(period)
    }

    /**
     * Oznaczenie przeterminowanych dokumentów.
     */
    @Transactional
    fun markOverdueDocuments() {
        logger.info("Marking overdue documents")

        val today = LocalDate.now()
        val overdueDocuments = documentRepository.findOverdueBefore(today)

        var marked = 0
        for (document in overdueDocuments) {
            if (document.status == DocumentStatus.NOT_PAID || document.status == DocumentStatus.PARTIALLY_PAID) {
                documentRepository.updateStatus(document.id, DocumentStatus.OVERDUE.name)
                marked++
            }
        }

        logger.info("Marked {} documents as overdue", marked)
    }

    /**
     * Konwertuje obiekt DTO na model domenowy dokumentu.
     */
    private fun convertToDocumentModel(request: CreateUnifiedDocumentRequest): UnifiedFinancialDocument {
        val now = LocalDateTime.now()
        return UnifiedFinancialDocument(
            id = UnifiedDocumentId.generate(),
            number = "", // Będzie wygenerowany przez repozytorium
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
            attachment = null, // Załącznik jest obsługiwany osobno
            audit = Audit(
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * Konwertuje obiekt DTO na model domenowy dokumentu (aktualizacja).
     */
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
            attachment = existingDocument.attachment, // Załącznik jest obsługiwany osobno
            audit = Audit(
                createdAt = existingDocument.audit.createdAt,
                updatedAt = LocalDateTime.now()
            )
        )
    }

    /**
     * Walidacja danych dokumentu.
     */
    private fun validateDocumentRequest(request: Any) {
        when (request) {
            is CreateUnifiedDocumentRequest -> {
                request.dueDate?.let { dueDate ->
                    if (dueDate.isBefore(request.issuedDate)) {
                        throw ValidationException("Due date cannot be before issued date")
                    }
                }

                // Sprawdzenie zgodności sum
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

                // Sprawdzenie zgodności sum
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

/**
 * Klasa pomocnicza dla paginacji wyników.
 */
data class PaginatedResult<T>(
    val data: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int
)