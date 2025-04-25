package com.carslab.crm.domain

import com.carslab.crm.api.model.*
import com.carslab.crm.api.model.request.CreateInvoiceRequest
import com.carslab.crm.api.model.request.UpdateInvoiceRequest
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.view.finance.*
import com.carslab.crm.domain.port.InvoiceRepository
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val invoiceStorageService: InvoiceStorageService
) {
    private val logger = LoggerFactory.getLogger(InvoiceService::class.java)

    /**
     * Tworzy nową fakturę.
     */
    @Transactional
    fun createInvoice(request: CreateInvoiceRequest, attachmentFile: MultipartFile?): Invoice {
        logger.info("Creating new invoice: {}", request.title)

        // Walidacja
        validateInvoiceRequest(request)

        // Tworzenie faktury z obiektu DTO
        val invoice = convertToInvoiceModel(request)

        // Obsługa załącznika, jeśli istnieje
        val attachment = attachmentFile?.let {
            val attachmentId = invoiceStorageService.storeInvoiceFile(it, invoice.id)
            InvoiceAttachment(
                name = it.originalFilename ?: "invoice.pdf",
                size = it.size,
                type = it.contentType ?: "application/octet-stream",
                storageId = attachmentId,
                uploadedAt = LocalDateTime.now()
            )
        }

        // Uzupełnienie faktury o załącznik
        val completeInvoice = if (attachment != null) {
            invoice.copy(attachment = attachment)
        } else {
            invoice
        }

        // Zapisanie faktury w repozytorium
        val savedInvoice = invoiceRepository.save(completeInvoice)
        logger.info("Created invoice with ID: {}", savedInvoice.id.value)

        return savedInvoice
    }

    /**
     * Aktualizuje istniejącą fakturę.
     */
    @Transactional
    fun updateInvoice(id: String, request: UpdateInvoiceRequest, attachmentFile: MultipartFile?): Invoice {
        logger.info("Updating invoice with ID: {}", id)

        // Sprawdzenie czy faktura istnieje
        val existingInvoice = invoiceRepository.findById(InvoiceId(id))
            ?: throw ResourceNotFoundException("Invoice", id)

        // Walidacja
        validateInvoiceRequest(request)

        // Konwersja DTO na model domenowy
        val updatedInvoice = convertToInvoiceModel(request, existingInvoice)

        // Obsługa załącznika
        val attachment = when {
            // Nowy załącznik
            attachmentFile != null -> {
                // Jeśli istniał poprzedni załącznik, usuń go
                existingInvoice.attachment?.let {
                    invoiceStorageService.deleteInvoiceFile(it.storageId)
                }

                // Zapisz nowy załącznik
                val attachmentId = invoiceStorageService.storeInvoiceFile(attachmentFile, existingInvoice.id)
                InvoiceAttachment(
                    name = attachmentFile.originalFilename ?: "invoice.pdf",
                    size = attachmentFile.size,
                    type = attachmentFile.contentType ?: "application/octet-stream",
                    storageId = attachmentId,
                    uploadedAt = LocalDateTime.now()
                )
            }
            // Zachowanie istniejącego załącznika
            else -> existingInvoice.attachment
        }

        // Uzupełnienie faktury o załącznik
        val completeInvoice = updatedInvoice.copy(attachment = attachment)

        // Zapisanie zaktualizowanej faktury
        val savedInvoice = invoiceRepository.save(completeInvoice)
        logger.info("Updated invoice with ID: {}", savedInvoice.id.value)

        return savedInvoice
    }

    /**
     * Pobiera fakturę po ID.
     */
    fun getInvoiceById(id: String): Invoice {
        logger.debug("Getting invoice by ID: {}", id)
        return invoiceRepository.findById(InvoiceId(id))
            ?: throw ResourceNotFoundException("Invoice", id)
    }

    /**
     * Pobiera wszystkie faktury z opcjonalnym filtrowaniem.
     */
    fun getAllInvoices(filter: InvoiceFilterDTO? = null): List<Invoice> {
        logger.debug("Getting all invoices with filter: {}", filter)
        return invoiceRepository.findAll(filter)
    }

    /**
     * Usuwa fakturę po ID.
     */
    @Transactional
    fun deleteInvoice(id: String): Boolean {
        logger.info("Deleting invoice with ID: {}", id)

        // Sprawdzenie czy faktura istnieje
        val invoice = invoiceRepository.findById(InvoiceId(id))
            ?: throw ResourceNotFoundException("Invoice", id)

        // Usunięcie załącznika, jeśli istnieje
        invoice.attachment?.let {
            invoiceStorageService.deleteInvoiceFile(it.storageId)
        }

        // Usunięcie faktury z repozytorium
        return invoiceRepository.deleteById(InvoiceId(id))
    }

    /**
     * Aktualizuje status faktury.
     */
    @Transactional
    fun updateInvoiceStatus(id: String, status: String): Boolean {
        logger.info("Updating status of invoice with ID: {} to: {}", id, status)

        // Sprawdzenie czy faktura istnieje
        if (invoiceRepository.findById(InvoiceId(id)) == null) {
            throw ResourceNotFoundException("Invoice", id)
        }

        return invoiceRepository.updateStatus(InvoiceId(id), status)
    }


    /**
     * Pobiera załącznik faktury.
     */
    fun getInvoiceAttachment(id: String): Pair<ByteArray, String>? {
        logger.debug("Getting attachment for invoice with ID: {}", id)

        // Sprawdzenie czy faktura istnieje
        val invoice = invoiceRepository.findById(InvoiceId(id))
            ?: throw ResourceNotFoundException("Invoice", id)

        // Jeśli faktura ma załącznik, pobierz go
        return invoice.attachment?.let {
            val fileBytes = invoiceStorageService.getInvoiceFile(it.storageId)
            fileBytes to it.type
        }
    }

    /**
     * Ekstrakcja danych z pliku faktury.
     */
    fun extractInvoiceData(file: MultipartFile): ExtractedInvoiceDataDTO {
        logger.info("Extracting data from invoice file: {}", file.originalFilename)

        // Tutaj w rzeczywistej implementacji byłaby integracja z usługą OCR
        // lub inną metodą ekstrakcji danych. Na potrzeby przykładu zwracamy mock.

        return ExtractedInvoiceDataDTO(
            generalInfo = GeneralInfoDTO(
                title = "Faktura za usługi detailingowe",
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
     * Oznaczenie przeterminowanych faktur.
     */
    @Transactional
    fun markOverdueInvoices() {
        logger.info("Marking overdue invoices")

        val today = LocalDate.now()
        val overdueInvoices = invoiceRepository.findOverdueBefore(today)

        var marked = 0
        for (invoice in overdueInvoices) {
            if (invoice.status == InvoiceStatus.NOT_PAID || invoice.status == InvoiceStatus.PARTIALLY_PAID) {
                invoiceRepository.updateStatus(invoice.id, InvoiceStatus.OVERDUE.name)
                marked++
            }
        }

        logger.info("Marked {} invoices as overdue", marked)
    }

    /**
     * Konwertuje obiekt DTO na model domenowy faktury.
     */
    private fun convertToInvoiceModel(request: CreateInvoiceRequest): Invoice {
        val now = LocalDateTime.now()
        return Invoice(
            id = InvoiceId.generate(),
            number = "", // Będzie wygenerowany przez repozytorium
            title = request.title,
            issuedDate = request.issuedDate,
            dueDate = request.dueDate,
            sellerName = request.sellerName,
            sellerTaxId = request.sellerTaxId,
            sellerAddress = request.sellerAddress,
            buyerName = request.buyerName,
            buyerTaxId = request.buyerTaxId,
            buyerAddress = request.buyerAddress,
            clientId = request.clientId?.let { ClientId(it) },
            status = InvoiceStatus.valueOf(request.status),
            type = InvoiceType.valueOf(request.type),
            paymentMethod = PaymentMethod.valueOf(request.paymentMethod),
            totalNet = request.totalNet,
            totalTax = request.totalTax,
            totalGross = request.totalGross,
            currency = request.currency,
            notes = request.notes,
            protocolId = request.protocolId,
            protocolNumber = request.protocolNumber,
            items = request.items.map { item ->
                InvoiceItem(
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
     * Konwertuje obiekt DTO na model domenowy faktury (aktualizacja).
     */
    private fun convertToInvoiceModel(request: UpdateInvoiceRequest, existingInvoice: Invoice): Invoice {
        return Invoice(
            id = existingInvoice.id,
            number = existingInvoice.number,
            title = request.title,
            issuedDate = request.issuedDate,
            dueDate = request.dueDate,
            sellerName = request.sellerName,
            sellerTaxId = request.sellerTaxId,
            sellerAddress = request.sellerAddress,
            buyerName = request.buyerName,
            buyerTaxId = request.buyerTaxId,
            buyerAddress = request.buyerAddress,
            clientId = request.clientId?.let { ClientId(it) },
            status = InvoiceStatus.valueOf(request.status),
            type = InvoiceType.valueOf(request.type),
            paymentMethod = PaymentMethod.valueOf(request.paymentMethod),
            totalNet = request.totalNet,
            totalTax = request.totalTax,
            totalGross = request.totalGross,
            currency = request.currency,
            notes = request.notes,
            protocolId = request.protocolId,
            protocolNumber = request.protocolNumber,
            items = request.items.map { item ->
                InvoiceItem(
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
            attachment = existingInvoice.attachment, // Załącznik jest obsługiwany osobno
            audit = Audit(
                createdAt = existingInvoice.audit.createdAt,
                updatedAt = LocalDateTime.now()
            )
        )
    }

    /**
     * Walidacja danych faktury.
     */
    private fun validateInvoiceRequest(request: Any) {
        when (request) {
            is CreateInvoiceRequest -> {
                if (request.dueDate.isBefore(request.issuedDate)) {
                    throw ValidationException("Due date cannot be before issued date")
                }

                // Sprawdzenie zgodności sum
                val calculatedTotalNet = request.items.sumOf { it.totalNet }
                val calculatedTotalGross = request.items.sumOf { it.totalGross }

                if (calculatedTotalNet.compareTo(request.totalNet) != 0) {
                    throw ValidationException("Total net amount does not match sum of items")
                }

                if (calculatedTotalGross.compareTo(request.totalGross) != 0) {
                    throw ValidationException("Total gross amount does not match sum of items")
                }
            }
            is UpdateInvoiceRequest -> {
                if (request.dueDate.isBefore(request.issuedDate)) {
                    throw ValidationException("Due date cannot be before issued date")
                }

                // Sprawdzenie zgodności sum
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