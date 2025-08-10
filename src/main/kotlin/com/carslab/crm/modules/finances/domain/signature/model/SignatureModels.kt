// Enhanced signature models with seller signature support
package com.carslab.crm.modules.finances.domain.signature.model

import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.signature.api.dto.SignatureSessionStatus
import java.time.Instant
import java.util.*

data class SignatureSessionRequest(
    val invoiceId: String,
    val tabletId: UUID,
    val companyId: Long,
    val signerName: String,
    val signatureTitle: String,
    val instructions: String?,
    val userId: String,
    val timeoutMinutes: Int
)

data class SignatureSession(
    val sessionId: UUID,
    val invoiceId: String,
    val tabletId: UUID,
    val companyId: Long,
    val signerName: String,
    val expiresAt: Instant,
    val status: SignatureSessionStatus
)

/**
 * Enhanced cached signature data with seller information
 */
data class CachedSignatureData(
    val sessionId: String,
    val document: UnifiedFinancialDocument,
    val originalPdfBytes: ByteArray,
    val signatureImageBytes: ByteArray,
    val signerName: String,
    val companyId: Long,
    val sellerId: Long = 1L // NEW: Seller ID for multi-tenant safety
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CachedSignatureData
        return sessionId == other.sessionId
    }

    override fun hashCode(): Int = sessionId.hashCode()
}

/**
 * Enhanced signature completion data with seller signature information
 */
data class SignatureCompletionData(
    val sessionId: String,
    val invoiceId: String,
    val success: Boolean,
    val signedAt: Instant,
    val signerName: String,
    val newAttachment: DocumentAttachment,
    val hasSellerSignature: Boolean = false // NEW: Indicates if seller signature is included
)

class InvoiceSignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)