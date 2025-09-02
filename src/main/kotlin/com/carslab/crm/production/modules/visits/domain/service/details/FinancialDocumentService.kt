package com.carslab.crm.production.modules.visits.domain.service.details

import com.carslab.crm.api.model.DocumentItemDTO
import com.carslab.crm.api.model.request.CreateUnifiedDocumentRequest
import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.modules.invoice_templates.domain.InvoiceTemplateService
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.domain.service.aggregate.VisitCompletionService
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class FinancialDocumentService(
    private val unifiedDocumentService: UnifiedDocumentService,
    private val companyDetailsFetchService: CompanyDetailsFetchService,
    private val visitDetailQueryService: VisitDetailQueryService,
    private val invoiceTemplateService: InvoiceTemplateService,
    private val securityContext: SecurityContext,
    private val universalStorageService: UniversalStorageService,
) {

    fun createVisitDocument(command: VisitCompletionService.FinancialDocumentCommand) {
        val visit = visitDetailQueryService.getVisitDetail(command.visitId)
        val companyDetails = companyDetailsFetchService.getCompanySettings(command.companyId)

        val documentItems = command.items.map { item ->
            DocumentItemDTO(
                id = null,
                name = item.name,
                description = null,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                taxRate = BigDecimal("23"),
                totalNet = item.totalNet,
                totalGross = item.totalGross
            )
        }

        val document = unifiedDocumentService.createDocument(
            request = CreateUnifiedDocumentRequest(
                type = command.documentType,
                title = "Opłata za wizytę - ${command.visitTitle}",
                description = command.additionalNotes,
                issuedDate = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(command.paymentDays.toLong()),
                sellerName = companyDetails.basicInfo?.companyName ?: "",
                sellerTaxId = companyDetails.basicInfo?.taxId ?: "",
                sellerAddress = companyDetails.basicInfo?.address ?: "",
                buyerName = visit.ownerName,
                buyerTaxId = visit.taxId,
                buyerAddress = visit.address,
                status = command.status.toString(),
                direction = "INCOME",
                paymentMethod = command.paymentMethod,
                totalNet = command.totals.totalNet,
                totalTax = command.totals.totalTax,
                totalGross = command.totals.totalGross,
                paidAmount = BigDecimal.ZERO,
                currency = "PLN",
                items = documentItems,
                notes = command.additionalNotes ?: "",
                protocolId = null,
                protocolNumber = null,
                visitId = command.visitId
            ),
            attachmentFile = null
        )
        if(command.documentType.uppercase() == "INVOICE") {
            val generated = invoiceTemplateService.generateInvoiceForDocument(
                securityContext.getCurrentCompanyId(),
                document.id.value,
            )
            val file = universalStorageService.storeFile(
                request = UniversalStoreRequest(
                    file = object : MultipartFile {
                        override fun getName(): String = "invoice.pdf"
                        override fun getOriginalFilename(): String = "invoice-${document.number}.pdf"
                        override fun getContentType(): String = "application/pdf"
                        override fun isEmpty(): Boolean = generated.isEmpty()
                        override fun getSize(): Long = generated.size.toLong()
                        override fun getBytes(): ByteArray = generated
                        override fun getInputStream(): java.io.InputStream = generated.inputStream()
                        override fun transferTo(dest: java.io.File): Unit =
                            throw UnsupportedOperationException("Transfer not supported")
                    },
                    originalFileName = "${document.number}.pdf",
                    contentType = "application/pdf",
                    companyId = command.companyId,
                    entityId = UUID.randomUUID().toString(),
                    entityType = "document",
                    category = "finances",
                    subCategory = "invoices/${document.direction.name.lowercase()}",
                    description = "",
                )
            )
            unifiedDocumentService.updateDocumentWithAttachment(
                document.copy(
                    attachment = DocumentAttachment(
                        name = "${document.number}.pdf",
                        size = generated.size.toLong(),
                        type = "application/pdf",
                        storageId = file,
                    )
                )
            )
        }
    }
}