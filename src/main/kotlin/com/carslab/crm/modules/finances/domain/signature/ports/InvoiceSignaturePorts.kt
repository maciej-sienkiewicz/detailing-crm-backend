package com.carslab.crm.modules.finances.domain.signature.ports

import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.modules.finances.domain.signature.model.SignatureSession
import com.carslab.crm.modules.finances.domain.signature.model.SignatureCompletionData
import java.util.*

interface InvoiceDocumentService {
    fun getDocument(invoiceId: String): UnifiedFinancialDocument
    fun createInvoiceFromVisit(visitId: String, companyId: Long, request: InvoiceSignatureRequest): UnifiedFinancialDocument
}

interface TabletCommunicationService {
    fun validateTabletAccess(tabletId: UUID, companyId: Long)
    fun sendSignatureRequest(session: SignatureSession, document: UnifiedFinancialDocument, pdfBytes: ByteArray): Boolean
    fun notifySessionCancellation(sessionId: UUID)
}

interface SignatureNotificationService {
    fun notifySignatureStarted(companyId: Long, sessionId: UUID, invoiceId: String)
    fun notifySignatureCompleted(companyId: Long, sessionId: String, data: SignatureCompletionData)
}

interface InvoiceAttachmentManager {
    fun getOrGenerateUnsignedPdf(document: UnifiedFinancialDocument): ByteArray
    fun generateSignedPdf(document: UnifiedFinancialDocument, signatureBytes: ByteArray): ByteArray
    fun replaceAttachment(document: UnifiedFinancialDocument, signedPdfBytes: ByteArray): com.carslab.crm.domain.model.view.finance.DocumentAttachment
    fun getOrGenerateUnsignedPdfWithSellerSignature(document: UnifiedFinancialDocument, sellerId: Long): ByteArray
    fun generateFullySignedPdf(
        document: UnifiedFinancialDocument,
        clientSignatureBytes: ByteArray,
        sellerId: Long
    ): ByteArray
}